package com.shieldguard.app.engine

object OtpGuard {
    private val OTP_PATTERNS = listOf(
        Regex("(?i)\\b(otp|one[ -]time[ -]password|verification code|security code|auth code|pin)\\b"),
        Regex("(?i)\\b(do not share|valid for \\d+ (mins|minutes|min)|expires in|never disclose)\\b"),
        Regex("(?i)\\b(credited by|debited by|transferred to|acct? ending|balance is|txn)\\b")
    )

    private val DIGIT_TOKEN = Regex("\\b\\d{4,8}\\b")

    /**
     * Checks if the message content looks like an authentic banking or authentication OTP.
     */
    fun isOtpOrTransactional(text: String): Boolean {
        if (text.isBlank()) return false
        val hasDigits = DIGIT_TOKEN.containsMatchIn(text)
        val matchesSecurityKeywords = OTP_PATTERNS.any { it.containsMatchIn(text) }
        return hasDigits && matchesSecurityKeywords
    }
}
