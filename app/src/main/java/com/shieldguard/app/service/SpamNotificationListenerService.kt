package com.shieldguard.app.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.shieldguard.app.data.local.entity.CommunicationChannel
import com.shieldguard.app.data.repository.ShieldRepository
import com.shieldguard.app.engine.FilterDecision
import com.shieldguard.app.engine.SpamEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpamNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var spamEngine: SpamEngine
    private lateinit var repository: ShieldRepository

    companion object {
        private const val PKG_WHATSAPP = "com.whatsapp"
        private const val PKG_WHATSAPP_BUSINESS = "com.whatsapp.w4b"
        private const val PKG_GMAIL = "com.google.android.gm"
        
        // Facebook family
        private const val PKG_FACEBOOK = "com.facebook.katana"
        private const val PKG_FACEBOOK_ORCA = "com.facebook.orca"
        private const val PKG_FACEBOOK_LITE = "com.facebook.lite"
        private const val PKG_MESSENGER_LITE = "com.facebook.mlite"

        // iQOO / Vivo Native Browser & web push
        private const val PKG_VIVO_BROWSER = "com.vivo.browser"
        private const val PKG_VIVO_HYBRID = "com.vivo.hybrid"
        private const val PKG_CHROME = "com.android.chrome"

        // iQOO / Vivo App Store & Game Center
        private const val PKG_VIVO_APPSTORE = "com.vivo.appstore"
        private const val PKG_BBK_APPSTORE = "com.bbk.appstore"
        private const val PKG_VIVO_GAME = "com.vivo.game"
        private const val PKG_VIVO_GAMEWATCH = "com.vivo.gamewatch"

        var isConnected: Boolean = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        spamEngine = SpamEngine(applicationContext)
        repository = ShieldRepository(applicationContext)
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

        // Do not intercept or block our own notifications!
        if (packageName == applicationContext.packageName) return

        val notification = sbn.notification ?: return
        
        // Map package to communication channel
        val channel = resolveChannel(packageName)

        // Comprehensive extraction of titles, texts, lines, messaging styles, subtext
        val (title, text) = extractNotificationContent(notification)

        if (title.isBlank() && text.isBlank()) return

        scope.launch {
            val decision = spamEngine.evaluateNotification(packageName, title, text)
            if (decision is FilterDecision.Block) {
                // Cancel/dismiss the notification immediately from notification drawer & lockscreen
                cancelNotification(sbn.key)

                // Log the blocked threat in Room DB
                repository.logBlockedEvent(
                    channel = channel,
                    sender = title.ifBlank { packageName },
                    snippet = text.take(200),
                    reason = decision.reason
                )
            }
        }
    }

    private fun resolveChannel(packageName: String): CommunicationChannel {
        return when (packageName) {
            PKG_WHATSAPP, PKG_WHATSAPP_BUSINESS -> CommunicationChannel.WHATSAPP
            PKG_GMAIL -> CommunicationChannel.GMAIL
            PKG_FACEBOOK, PKG_FACEBOOK_ORCA, PKG_FACEBOOK_LITE, PKG_MESSENGER_LITE -> CommunicationChannel.FACEBOOK
            PKG_VIVO_BROWSER, PKG_VIVO_HYBRID, PKG_CHROME, "com.heytap.browser", "com.coloros.browser", "com.opera.browser" -> CommunicationChannel.BROWSER
            PKG_VIVO_APPSTORE, PKG_BBK_APPSTORE, PKG_VIVO_GAME, PKG_VIVO_GAMEWATCH, "com.heytap.market" -> CommunicationChannel.APP_STORE
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
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (textLines != null) {
            for (line in textLines) {
                textBuilder.append(line).append(" ")
            }
        }

        // Android MessagingStyle messages (WhatsApp, Messenger)
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null) {
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
