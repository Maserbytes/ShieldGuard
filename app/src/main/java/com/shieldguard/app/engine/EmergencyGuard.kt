package com.shieldguard.app.engine

import android.content.Context
import android.os.Build
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager

object EmergencyGuard {
    private val COMMON_EMERGENCY_NUMBERS = setOf("911", "112", "999", "000", "110", "119", "100", "101", "102")

    /**
     * Regulatory Safety Check: An emergency number MUST NEVER be blocked or screened.
     */
    fun isEmergency(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

        if (COMMON_EMERGENCY_NUMBERS.contains(cleanNumber)) return true

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                telephonyManager?.isEmergencyNumber(cleanNumber) ?: PhoneNumberUtils.isEmergencyNumber(cleanNumber)
            } else {
                @Suppress("DEPRECATION")
                PhoneNumberUtils.isEmergencyNumber(cleanNumber)
            }
        } catch (e: Exception) {
            COMMON_EMERGENCY_NUMBERS.contains(cleanNumber)
        }
    }
}
