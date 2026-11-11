package com.shieldguard.app.service

import android.app.Notification
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.shieldguard.app.data.local.entity.CommunicationChannel
import com.shieldguard.app.data.repository.ShieldRepository
import com.shieldguard.app.engine.FilterDecision
import com.shieldguard.app.engine.KnownPackages
import com.shieldguard.app.engine.SpamEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SpamNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var spamEngine: SpamEngine
    private lateinit var repository: ShieldRepository

    /**
     * Keys we already cancelled, with the content that triggered it.
     *
     * Messaging apps re-post the same notification for every delivery receipt, edit and typing
     * state change. Without this, one spam message would be evaluated (and logged) dozens of
     * times, and a group summary could be cancelled repeatedly until the whole chat disappears
     * from the shade.
     */
    private val recentlyCancelled = object : LinkedHashMap<String, Long>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?) =
            size > MAX_TRACKED_KEYS
    }

    companion object {
        private const val MAX_TRACKED_KEYS = 128
        private const val CANCEL_DEBOUNCE_MS = 1_500L

        var isConnected: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        spamEngine = SpamEngine(applicationContext)
        repository = ShieldRepository(applicationContext)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val packageName = sbn.packageName ?: return

        // Never touch our own notifications.
        if (packageName == applicationContext.packageName) return

        // Ongoing notifications belong to foreground services and active calls. Cancelling them
        // can kill live UI (music controls, navigation, a call in progress).
        if (sbn.isOngoing) return

        val notification = sbn.notification ?: return

        // Group summaries are containers. Dropping them removes the whole conversation from the
        // shade even when only one message inside it was spam, so they are left alone.
        if (isGroupSummary(notification)) return

        val channel = resolveChannel(packageName)
        val (title, text) = extractNotificationContent(notification)
        if (title.isBlank() && text.isBlank()) return

        val isGroupConversation = extractGroupFlag(notification)

        scope.launch {
            val decision = spamEngine.evaluateNotification(
                packageName = packageName,
                title = title,
                text = text,
                isGroupConversation = isGroupConversation,
            )

            if (decision is FilterDecision.Block) {
                val fingerprint = "${sbn.key}#${text.hashCode()}"
                val now = System.currentTimeMillis()
                synchronized(recentlyCancelled) {
                    val last = recentlyCancelled[fingerprint]
                    if (last != null && now - last < CANCEL_DEBOUNCE_MS) return@launch
                    recentlyCancelled[fingerprint] = now
                }

                cancelNotification(sbn.key)

                repository.logBlockedEvent(
                    channel = channel,
                    sender = title.ifBlank { packageName },
                    snippet = text.take(200),
                    reason = decision.reason,
                )
            }
        }
    }

    private fun isGroupSummary(notification: Notification): Boolean =
        (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

    private fun extractGroupFlag(notification: Notification): Boolean? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        val extras = notification.extras ?: return null
        if (!extras.containsKey(Notification.EXTRA_IS_GROUP_CONVERSATION)) return null
        return extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION)
    }

    private fun resolveChannel(packageName: String): CommunicationChannel {
        return when (packageName) {
            KnownPackages.WHATSAPP, KnownPackages.WHATSAPP_BUSINESS -> CommunicationChannel.WHATSAPP
            KnownPackages.GMAIL -> CommunicationChannel.GMAIL
            KnownPackages.FACEBOOK, KnownPackages.FACEBOOK_ORCA,
            KnownPackages.FACEBOOK_LITE, KnownPackages.MESSENGER_LITE -> CommunicationChannel.FACEBOOK
            in KnownPackages.BROWSERS -> CommunicationChannel.BROWSER
            in KnownPackages.APP_STORES -> CommunicationChannel.APP_STORE
            else -> CommunicationChannel.OTHER
        }
    }

    private fun extractNotificationContent(notification: Notification): Pair<String, String> {
        val extras = notification.extras ?: return "" to ""

        val title = (extras.getCharSequence(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
            ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
            ?: "").toString()

        val textBuilder = StringBuilder()

        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { textBuilder.append(it).append(" ") }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { textBuilder.append(it).append(" ") }
        extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.let { textBuilder.append(it).append(" ") }
        extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.let { textBuilder.append(it).append(" ") }
        extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.let { textBuilder.append(it).append(" ") }

        // WhatsApp group messages & Gmail bundle lines
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.let { lines ->
            for (line in lines) textBuilder.append(line).append(" ")
        }

        // Android MessagingStyle messages (WhatsApp, Messenger)
        extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.let { messages ->
            for (msg in messages) {
                if (msg is Bundle) {
                    msg.getCharSequence("text")?.let { textBuilder.append(it).append(" ") }
                    msg.getCharSequence("sender")?.let { textBuilder.append(it).append(" ") }
                }
            }
        }

        notification.tickerText?.let { textBuilder.append(it).append(" ") }

        return title.trim() to textBuilder.toString().trim()
    }
}
