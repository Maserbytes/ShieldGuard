package com.shieldguard.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.shieldguard.app.data.local.AppDatabase
import com.shieldguard.app.service.ShieldForegroundService
import com.shieldguard.app.worker.RuleSyncWorker

class ShieldGuardApp : Application() {
    companion object {
        const val ALERT_CHANNEL_ID = "shield_guard_alerts_channel"
    }

    override fun onCreate() {
        super.onCreate()
        
        // 1. Initialize local SQLite/Room database
        AppDatabase.getInstance(this)

        // 2. Register Notification Channels
        createNotificationChannels()

        // 3. Schedule $0 periodic community rules sync via WorkManager
        RuleSyncWorker.schedule(this)

        // 4. Start foreground protection
        ShieldForegroundService.start(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                ALERT_CHANNEL_ID,
                getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.channel_alerts_desc)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(alertChannel)
        }
    }
}
