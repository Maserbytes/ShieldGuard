package com.shieldguard.app.service

import android.telecom.Call
import android.telecom.CallScreeningService
import com.shieldguard.app.data.local.entity.CommunicationChannel
import com.shieldguard.app.data.repository.ShieldRepository
import com.shieldguard.app.engine.FilterDecision
import com.shieldguard.app.engine.SpamEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SpamCallScreeningService : CallScreeningService() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private lateinit var spamEngine: SpamEngine
    private lateinit var repository: ShieldRepository

    override fun onCreate() {
        super.onCreate()
        spamEngine = SpamEngine(applicationContext)
        repository = ShieldRepository(applicationContext)
    }

    override fun onScreenCall(callDetails: Call.Details) {
        val number = callDetails.handle?.schemeSpecificPart ?: ""
        
        scope.launch {
            val decision = spamEngine.evaluateCall(number)

            val responseBuilder = CallResponse.Builder()
            when (decision) {
                is FilterDecision.Block -> {
                    responseBuilder.apply {
                        setDisallowCall(true)
                        setRejectCall(true)
                        setSkipCallLog(false)
                        setSkipNotification(true)
                    }
                    repository.logBlockedEvent(
                        channel = CommunicationChannel.CALL,
                        sender = number.ifBlank { "Private / Unknown Number" },
                        snippet = "Incoming call dropped by ShieldGuard",
                        reason = decision.reason
                    )
                }
                is FilterDecision.Silence -> {
                    responseBuilder.apply {
                        setDisallowCall(false)
                        setRejectCall(false)
                        setSilenceCall(true)
                    }
                    repository.logBlockedEvent(
                        channel = CommunicationChannel.CALL,
                        sender = number.ifBlank { "Private / Unknown Number" },
                        snippet = "Incoming call silenced",
                        reason = decision.reason
                    )
                }
                is FilterDecision.Allow -> {
                    responseBuilder.apply {
                        setDisallowCall(false)
                        setRejectCall(false)
                    }
                }
            }

            respondToCall(callDetails, responseBuilder.build())
        }
    }
}
