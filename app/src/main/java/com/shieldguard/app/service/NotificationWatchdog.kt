package com.shieldguard.app.service

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object NotificationWatchdog {

    /**
     * Checks if the app is granted Notification Listener access in Android Settings.
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(context.packageName)
    }

    /**
     * Resets/re-triggers the Android OS Notification Listener binding handshake.
     * Crucial for aggressive OEM ROMs (MIUI, ColorOS, FunTouch, OneUI).
     */
    fun ensureServiceAlive(context: Context) {
        if (!isNotificationAccessGranted(context)) return

        if (!SpamNotificationListenerService.isConnected) {
            val componentName = ComponentName(context, SpamNotificationListenerService::class.java)
            val pm = context.packageManager
            try {
                pm.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
