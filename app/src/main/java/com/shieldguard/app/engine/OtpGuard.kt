package com.shieldguard.app.engine

/**
 * Tier 1 protection: OTPs, verification codes and transactional bank alerts must never be
 * filtered, no matter which channel or rule would otherwise match them.
 *
 * Pure Kotlin, no Android imports, so it is covered directly by JVM unit tests.
 */
object OtpGuard {

    private val SECURITY_KEYWORDS = listOf(
        Regex("(?i)\\b(otp|one[ -]?time[ -]?(password|code)|verification code|security code|auth(entication)? code|passcode|pin)\\b"),
        Regex("(?i)\\b(do not share|never disclose|do not disclose|valid for \\d+ ?(mins?|minutes?)|expires in \\d+)\\b"),
        Regex("(?i)\\b(credited by|debited by|transferred to|acct? ending|a/c ending|balance is|available balance|txn|transaction)\\b"),
    )

    /** 4-8 digit codes, and alphanumeric codes such as `A1B2C3` that still contain a digit. */
    private val NUMERIC_CODE = Regex("(?<![\\d])\\d{4,8}(?![\\d])")
    private val ALPHANUMERIC_CODE = Regex("\\b(?=[A-Za-z0-9]{4,8}\\b)(?=[A-Za-z0-9]*\\d)[A-Za-z0-9]{4,8}\\b")

    /**
     * True when [text] reads like an authentication or transactional message.
     *
     * Intentionally biased towards false negatives: letting one spam message through costs a
     * swipe, while eating a real OTP costs the user an account.
     */
    fun isOtpOrTransactional(text: String): Boolean {
        if (text.isBlank()) return false
        if (SECURITY_KEYWORDS.none { it.containsMatchIn(text) }) return false
        return NUMERIC_CODE.containsMatchIn(text) || ALPHANUMERIC_CODE.containsMatchIn(text)
    }
}
