package com.shieldguard.app.engine

import android.content.Context
import com.shieldguard.app.data.local.entity.AllowlistType
import com.shieldguard.app.data.local.entity.RuleAction
import com.shieldguard.app.data.local.entity.RuleType
import com.shieldguard.app.data.repository.ShieldRepository

class SpamEngine(private val context: Context) {
    private val repository = ShieldRepository(context)

    suspend fun evaluateCall(rawNumber: String): FilterDecision {
        if (rawNumber.isBlank()) return FilterDecision.Allow

        // 1. Never block emergency numbers
        if (EmergencyGuard.isEmergency(context, rawNumber)) {
            return FilterDecision.Allow
        }

        // 2. Allow if saved in user's contacts
        if (repository.isContact(rawNumber)) {
            return FilterDecision.Allow
        }

        // 3. Allow if on explicit allowlist
        if (repository.isExplicitlyAllowed(rawNumber, AllowlistType.PHONE_NUMBER)) {
            return FilterDecision.Allow
        }

        // 4. Test active rules
        val activeRules = repository.getActiveRules()
        for (rule in activeRules) {
            if (rule.type == RuleType.CALL_REGEX) {
                try {
                    val regex = Regex(rule.pattern)
                    if (regex.containsMatchIn(rawNumber)) {
                        return when (rule.action) {
                            RuleAction.BLOCK -> FilterDecision.Block(rule.description.ifBlank { "Matched pattern: ${rule.pattern}" })
                            RuleAction.SILENCE -> FilterDecision.Silence(rule.description.ifBlank { "Silenced by rule: ${rule.pattern}" })
                            RuleAction.ALLOW -> FilterDecision.Allow
                        }
                    }
                } catch (e: Exception) {
                    // Ignore malformed regex gracefully
                }
            }
        }

        return FilterDecision.Allow
    }

    suspend fun evaluateSms(sender: String, messageBody: String): FilterDecision {
        // 1. Allow if contact
        if (repository.isContact(sender)) {
            return FilterDecision.Allow
        }

        // 2. Allow if sender header or number is allowlisted
        if (repository.isExplicitlyAllowed(sender, AllowlistType.SENDER_HEADER) ||
            repository.isExplicitlyAllowed(sender, AllowlistType.PHONE_NUMBER)
        ) {
            return FilterDecision.Allow
        }

        // 3. High Priority: Never block OTPs or transactional messages
        if (OtpGuard.isOtpOrTransactional(messageBody)) {
            return FilterDecision.Allow
        }

        // 4. Evaluate Rules (Keywords and Regex)
        val activeRules = repository.getActiveRules()
        for (rule in activeRules) {
            when (rule.type) {
                RuleType.WHITELIST_REGEX -> {
                    try {
                        if (Regex(rule.pattern).containsMatchIn(messageBody)) {
                            return FilterDecision.Allow
                        }
                    } catch (_: Exception) {}
                }
                RuleType.SMS_REGEX -> {
                    try {
                        if (Regex(rule.pattern).containsMatchIn(messageBody)) {
                            return FilterDecision.Block(rule.description.ifBlank { "Matched SMS Regex" })
                        }
                    } catch (_: Exception) {}
                }
                RuleType.SMS_KEYWORD -> {
                    try {
                        if (Regex(rule.pattern).containsMatchIn(messageBody)) {
                            return FilterDecision.Block(rule.description.ifBlank { "Spam keyword detected" })
                        }
                    } catch (_: Exception) {}
                }
                else -> {}
            }
        }

        // 5. Heuristic check: Suspicious URLs + High urgency words
        if (hasSuspiciousPatterns(messageBody)) {
            return FilterDecision.Block("Heuristic: Suspicious phishing link or scam pattern")
        }

        return FilterDecision.Allow
    }

    suspend fun evaluateNotification(packageName: String, title: String, text: String): FilterDecision {
        val fullContent = "$title $text"

        // 1. High Priority: Never block 2FA / OTP codes delivered via WhatsApp or Gmail
        if (OtpGuard.isOtpOrTransactional(fullContent)) {
            return FilterDecision.Allow
        }

        // 2. Evaluate rules
        val activeRules = repository.getActiveRules()
        for (rule in activeRules) {
            if (rule.type == RuleType.NOTIFICATION_KEYWORD || rule.type == RuleType.NOTIFICATION_REGEX) {
                try {
                    if (Regex(rule.pattern).containsMatchIn(fullContent)) {
                        return FilterDecision.Block(rule.description.ifBlank { "Matched Notification Rule" })
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. Check keywords for WhatsApp & Gmail promotional spam
        if (hasPromotionalKeywords(fullContent)) {
            return FilterDecision.Block("Promotional spam notification")
        }

        return FilterDecision.Allow
    }

    private fun hasSuspiciousPatterns(text: String): Boolean {
        val hasLink = text.contains("http://") || text.contains("https://") || text.contains("bit.ly") || text.contains("tinyurl.com")
        val isScammy = text.contains("account suspended", ignoreCase = true) ||
                text.contains("click here to verify", ignoreCase = true) ||
                text.contains("lottery prize", ignoreCase = true) ||
                text.contains("claim bonus", ignoreCase = true)
        return hasLink && isScammy
    }

    private fun hasPromotionalKeywords(text: String): Boolean {
        val spamWords = listOf("50% off", "flat off", "use code", "buy 1 get 1", "exclusive discount", "free spin", "earn money daily")
        return spamWords.any { text.contains(it, ignoreCase = true) }
    }
}
