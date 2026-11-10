package com.shieldguard.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shieldguard.app.service.NotificationWatchdog
import com.shieldguard.app.service.ShieldForegroundService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            // Re-arm the foreground service
            ShieldForegroundService.start(context)

            // Re-arm notification listener
            NotificationWatchdog.ensureServiceAlive(context)
        }
    }
}
