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
                    // SMS_RECEIVED is an ordered broadcast: aborting here stops the message from
                    // reaching the system's default SMS parser and inbox.
                    abortBroadcast()

                    repository.logBlockedEvent(
                        channel = CommunicationChannel.SMS,
                        sender = sender,
                        snippet = body.take(150),
                        reason = "${decision.reason} (secondary listener)",
                    )
                }
            } catch (e: Exception) {
                // Fail open: if a rule blows up, the message must still reach the user.
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
