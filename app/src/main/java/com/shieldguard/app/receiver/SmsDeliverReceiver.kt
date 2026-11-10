package com.shieldguard.app.receiver

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import com.shieldguard.app.data.local.entity.CommunicationChannel
import com.shieldguard.app.data.repository.ShieldRepository
import com.shieldguard.app.engine.FilterDecision
import com.shieldguard.app.engine.SpamEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Primary SMS Receiver for the Default SMS App role.
 * Only the currently active Default SMS application receives SMS_DELIVER intents.
 */
class SmsDeliverReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: ""
        val bodyBuilder = StringBuilder()
        for (msg in messages) {
            bodyBuilder.append(msg.displayMessageBody)
        }
        val fullBody = bodyBuilder.toString()

        val pendingResult = goAsync()
        val spamEngine = SpamEngine(context.applicationContext)
        val repository = ShieldRepository(context.applicationContext)

        scope.launch {
            try {
                val decision = spamEngine.evaluateSms(sender, fullBody)
                when (decision) {
                    is FilterDecision.Block -> {
                        // Dropped / Halted from reaching native user inbox
                        repository.logBlockedEvent(
                            channel = CommunicationChannel.SMS,
                            sender = sender,
                            snippet = fullBody.take(150),
                            reason = decision.reason
                        )
                    }
                    is FilterDecision.Silence, is FilterDecision.Allow -> {
                        // Safe message: persist to system SMS Inbox provider
                        saveSmsToInbox(context, sender, fullBody, messages[0].timestampMillis)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun saveSmsToInbox(context: Context, sender: String, body: String, timestamp: Long) {
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, sender)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, timestamp)
                put(Telephony.Sms.READ, 0)
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
            }
            context.contentResolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
