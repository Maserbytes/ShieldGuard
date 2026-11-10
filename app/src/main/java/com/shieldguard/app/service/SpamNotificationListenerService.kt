package com.shieldguard.app.service

import android.app.Notification
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

        // Intercept WhatsApp, WhatsApp Business and Gmail
        val channel = when (packageName) {
            PKG_WHATSAPP, PKG_WHATSAPP_BUSINESS -> CommunicationChannel.WHATSAPP
            PKG_GMAIL -> CommunicationChannel.GMAIL
            else -> return
        }

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        scope.launch {
            val decision = spamEngine.evaluateNotification(packageName, title, text)
            if (decision is FilterDecision.Block) {
                // Cancel/dismiss the notification from the user's notification drawer & lockscreen
                cancelNotification(sbn.key)

                // Log the blocked event for the user
                repository.logBlockedEvent(
                    channel = channel,
                    sender = title.ifBlank { packageName },
                    snippet = text.take(150),
                    reason = decision.reason
                )
            }
        }
    }
}
