package com.shieldguard.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Required system service for Default SMS App compliance.
 * Handles "Respond via message" requests (e.g. quick reply to a rejected incoming phone call).
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle background quick response if required
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
