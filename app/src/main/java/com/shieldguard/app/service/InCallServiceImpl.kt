package com.shieldguard.app.service

import android.telecom.Call
import android.telecom.InCallService

/**
 * Required implementation for Default Dialer role.
 * Connects to the Android Telecom framework to monitor and manage calls.
 */
class InCallServiceImpl : InCallService() {

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call?, state: Int) {
            super.onStateChanged(call, state)
            // State updates (Ringing, Active, Disconnected)
        }
    }

    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)
        call?.registerCallback(callCallback)
    }

    override fun onCallRemoved(call: Call?) {
        super.onCallRemoved(call)
        call?.unregisterCallback(callCallback)
    }
}
