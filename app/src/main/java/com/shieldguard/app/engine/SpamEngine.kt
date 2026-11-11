package com.shieldguard.app.engine

import android.content.Context
import com.shieldguard.app.data.local.entity.AllowlistType
import com.shieldguard.app.data.local.entity.RuleAction
import com.shieldguard.app.data.local.entity.RuleType
import com.shieldguard.app.data.repository.ShieldRepository

class SpamEngine(private val context: Context) {
    private val repository = ShieldRepository(context)

    companion object {
        // Known packages
        private const val PKG_WHATSAPP = "com.whatsapp"
        private const val PKG_WHATSAPP_BUSINESS = "com.whatsapp.w4b"
        private const val PKG_GMAIL = "com.google.android.gm"
        private const val PKG_FACEBOOK = "com.facebook.katana"
        private const val PKG_FACEBOOK_ORCA = "com.facebook.orca"
        private const val PKG_FACEBOOK_LITE = "com.facebook.lite"
        private const val PKG_MESSENGER_LITE = "com.facebook.mlite"
        private const val PKG_VIVO_BROWSER = "com.vivo.browser"
        private const val PKG_VIVO_HYBRID = "com.vivo.hybrid"
        private const val PKG_VIVO_APPSTORE = "com.vivo.appstore"
        private const val PKG_BBK_APPSTORE = "com.bbk.appstore"
        private const val PKG_VIVO_GAME = "com.vivo.game"
        private const val PKG_VIVO_GAMEWATCH = "com.vivo.gamewatch"

        // 1. WhatsApp Product, Deals & Group Commercial Spam Patterns
        private val WHATSAPP_PRODUCT_SPAM_REGEX = Regex(
            "(?i)\\b(" +
            "deals?|discounts?|flat \\d+%|clearance|sale|coupon( code)?|promo code|" +
            "order now|free delivery|cash on delivery|cod available|buy \\d+ get \\d+|" +
            "lowest price|price drop|wholesale|reseller|stock available|limited stock|" +
            "dm for price|dm to order|link in bio|check out this product|hot deal|" +
            "amazon deal|flipkart deal|meesho|loot deal|grab now|flash sale|" +
            "pre-order|free shipping|exclusive offer|use coupon code|price:?\\s*₹?\\d+|" +
            "catalogue|catalog|bestseller|limited offer|offer valid|diwali offer|" +
            "festival sale|big billion|mega sale|festive sale|hurry up" +
            ")\\b"
        )

        // 2. Facebook Friend Suggestions, Congrats & Unsolicited Engagement
        private val FB_FRIEND_SUGGESTION_REGEX = Regex(
            "(?i)\\b(" +
            "people you may know|friend suggestions?|suggested friends?|suggested for you|" +
            "add as friend|sent you a wave|suggests you add|friend request from someone you may know" +
            ")\\b"
        )

        private val FB_CONGRATS_AND_MILESTONE_REGEX = Regex(
            "(?i)\\b(" +
            "say congrats|congratulate|congratulations on|wishing .* a happy birthday|" +
            "it's .*'s birthday|happy birthday|birthday wish|celebrating .* years? on facebook|" +
            "shared a memory|memories from this day|you have a new memory|look back at" +
            ")\\b"
        )

        private val FB_ENGAGEMENT_BAIT_REGEX = Regex(
            "(?i)\\b(" +
            "new posts? from|trending on facebook|shared a (photo|video|reel|post)|" +
            "recently shared|you have (new )?updates?|reels? for you|suggested group|" +
            "join group|suggested post|post you might like|trending video" +
            ")\\b"
        )

        // 3. Discord Mentions via Gmail
        private val DISCORD_GMAIL_REGEX = Regex(
            "(?i)\\b(discord)\\b.*\\b(" +
            "mentioned you|sent a message|new message|@everyone|@here|server|channel|" +
            "discordapp|notification@discord" +
            ")\\b"
        )

        // 4. iQOO / Vivo Browser News & Clickbait Bloatware
        private val BROWSER_CLICKBAIT_REGEX = Regex(
            "(?i)\\b(" +
            "breaking news|shocking|must watch|spin the wheel|win cash|daily horoscope|" +
            "viral video|trending news|click to read|exclusive reveal|top stories|" +
            "you won't believe|see what happened|latest gossip" +
            ")\\b"
        )

        // 5. iQOO / Vivo App Store & Game Center Ads
        private val APP_STORE_PROMO_REGEX = Regex(
            "(?i)\\b(" +
            "popular game|trending apps?|boost your phone|clean up|free gifts?|" +
            "coins? reward|daily check-in|must-have apps?|recommended apps?|" +
            "download now|install to earn|hot apps|update ready" +
            ")\\b"
        )

        // 6. Generic Promotional Spam Keywords
        private val GENERIC_PROMO_KEYWORDS = listOf(
            "50% off", "flat off", "use code", "buy 1 get 1", "exclusive discount",
            "free spin", "earn money daily", "claim your prize", "congratulations you won",
            "unlimited cashback", "scratch card", "instant loan", "zero interest loan"
        )
    }

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

        // 1. High Priority: Never block 2FA / OTP codes delivered via any channel
        if (OtpGuard.isOtpOrTransactional(fullContent)) {
            return FilterDecision.Allow
        }

        // 2. Evaluate explicit database rules first
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

        // 3. WhatsApp Group Product & Commercial Spam
        if (packageName == PKG_WHATSAPP || packageName == PKG_WHATSAPP_BUSINESS) {
            if (WHATSAPP_PRODUCT_SPAM_REGEX.containsMatchIn(fullContent)) {
                return FilterDecision.Block("WhatsApp product & deal spam")
            }
            if (hasPriceAndPromoPattern(fullContent)) {
                return FilterDecision.Block("WhatsApp commercial price offer")
            }
        }

        // 4. Facebook Friend Suggestions, Congrats & Unsolicited Milestones
        if (packageName == PKG_FACEBOOK || packageName == PKG_FACEBOOK_ORCA ||
            packageName == PKG_FACEBOOK_LITE || packageName == PKG_MESSENGER_LITE
        ) {
            if (FB_FRIEND_SUGGESTION_REGEX.containsMatchIn(fullContent)) {
                return FilterDecision.Block("Facebook friend suggestion")
            }
            if (FB_CONGRATS_AND_MILESTONE_REGEX.containsMatchIn(fullContent)) {
                return FilterDecision.Block("Facebook congrats / milestone notification")
            }
            if (FB_ENGAGEMENT_BAIT_REGEX.containsMatchIn(fullContent)) {
                return FilterDecision.Block("Facebook random reels & activity prompt")
            }
        }

        // 5. Discord Mentions via Gmail
        if (packageName == PKG_GMAIL) {
            if (DISCORD_GMAIL_REGEX.containsMatchIn(fullContent) ||
                (fullContent.contains("Discord", ignoreCase = true) && fullContent.contains("mentioned you", ignoreCase = true))
            ) {
                return FilterDecision.Block("Discord mention via Gmail")
            }
        }

        // 6. iQOO / Vivo Native Browser Bloatware & Push Notifications
        if (packageName == PKG_VIVO_BROWSER || packageName == PKG_VIVO_HYBRID) {
            val isDownloadNotification = fullContent.contains("download", ignoreCase = true) &&
                (fullContent.contains("complete", ignoreCase = true) || fullContent.contains("progress", ignoreCase = true) || fullContent.contains("finished", ignoreCase = true))
            
            if (!isDownloadNotification) {
                return FilterDecision.Block("iQOO native browser push spam")
            }
        }

        // 7. iQOO / Vivo App Store & Game Center Ads
        if (packageName == PKG_VIVO_APPSTORE || packageName == PKG_BBK_APPSTORE ||
            packageName == PKG_VIVO_GAME || packageName == PKG_VIVO_GAMEWATCH
        ) {
            val isActualInstall = fullContent.contains("installing", ignoreCase = true) &&
                !fullContent.contains("recommended", ignoreCase = true)
            
            if (!isActualInstall || APP_STORE_PROMO_REGEX.containsMatchIn(fullContent)) {
                return FilterDecision.Block("iQOO App Store promotional ad")
            }
        }

        // 8. General Clickbait / News in other browsers
        if (BROWSER_CLICKBAIT_REGEX.containsMatchIn(fullContent)) {
            return FilterDecision.Block("News clickbait notification")
        }

        // 9. Generic Promotional Spam Keywords
        if (hasPromotionalKeywords(fullContent)) {
            return FilterDecision.Block("Promotional spam notification")
        }

        return FilterDecision.Allow
    }

    private fun hasPriceAndPromoPattern(text: String): Boolean {
        val hasPrice = text.contains("₹") || text.contains("rs.", ignoreCase = true) || text.contains("inr", ignoreCase = true) || text.contains("$")
        val hasOffer = text.contains("off", ignoreCase = true) || text.contains("only", ignoreCase = true) ||
                text.contains("free", ignoreCase = true) || text.contains("price", ignoreCase = true) ||
                text.contains("order", ignoreCase = true)
        return hasPrice && hasOffer
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
        return GENERIC_PROMO_KEYWORDS.any { text.contains(it, ignoreCase = true) }
    }
}
