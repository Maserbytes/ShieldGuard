package com.shieldguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.shieldguard.app.data.local.entity.CommunicationChannel
import com.shieldguard.app.data.repository.ShieldRepository
import com.shieldguard.app.engine.FilterDecision
import com.shieldguard.app.engine.SpamEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Secondary SMS Receiver for non-default mode.
 * Can read incoming SMS and trigger warning alerts if not set as default.
 */
class SmsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: ""
        val body = messages.joinToString("") { it.displayMessageBody ?: "" }

        val pendingResult = goAsync()
        val spamEngine = SpamEngine(context.applicationContext)
        val repository = ShieldRepository(context.applicationContext)

        scope.launch {
            try {
                val decision = spamEngine.evaluateSms(sender, body)
                if (decision is FilterDecision.Block) {
                    repository.logBlockedEvent(
                        channel = CommunicationChannel.SMS,
                        sender = sender,
                        snippet = body.take(150),
                        reason = "${decision.reason} (Detected via secondary listener)"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
