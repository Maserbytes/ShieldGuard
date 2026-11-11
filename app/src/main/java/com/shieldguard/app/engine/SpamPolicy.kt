package com.shieldguard.app.engine

import com.shieldguard.app.data.local.entity.AllowlistType
import com.shieldguard.app.data.local.entity.RuleAction
import com.shieldguard.app.data.local.entity.RuleType

/**
 * Package names ShieldGuard has an opinion about.
 *
 * The list lives here, in the pure layer, because both the notification listener and the
 * decision core need it. It used to be duplicated in two files, where it had already drifted.
 */
object KnownPackages {
    const val WHATSAPP = "com.whatsapp"
    const val WHATSAPP_BUSINESS = "com.whatsapp.w4b"
    const val GMAIL = "com.google.android.gm"

    const val FACEBOOK = "com.facebook.katana"
    const val FACEBOOK_ORCA = "com.facebook.orca"
    const val FACEBOOK_LITE = "com.facebook.lite"
    const val MESSENGER_LITE = "com.facebook.mlite"

    const val VIVO_BROWSER = "com.vivo.browser"
    const val VIVO_HYBRID = "com.vivo.hybrid"
    const val CHROME = "com.android.chrome"
    const val HEYTAP_BROWSER = "com.heytap.browser"
    const val COLOROS_BROWSER = "com.coloros.browser"
    const val OPERA_BROWSER = "com.opera.browser"

    const val VIVO_APPSTORE = "com.vivo.appstore"
    const val BBK_APPSTORE = "com.bbk.appstore"
    const val VIVO_GAME = "com.vivo.game"
    const val VIVO_GAMEWATCH = "com.vivo.gamewatch"
    const val HEYTAP_MARKET = "com.heytap.market"

    /** Apps where the notification title is a real human being you may know. */
    val MESSAGING: Set<String> = setOf(
        WHATSAPP, WHATSAPP_BUSINESS, FACEBOOK_ORCA, FACEBOOK_LITE, MESSENGER_LITE
    )

    /** Apps whose notifications are news/clickbait firehoses. */
    val BROWSERS: Set<String> = setOf(
        VIVO_BROWSER, VIVO_HYBRID, CHROME, HEYTAP_BROWSER, COLOROS_BROWSER, OPERA_BROWSER
    )

    /** Apps that push promotional product ads. */
    val APP_STORES: Set<String> = setOf(
        VIVO_APPSTORE, BBK_APPSTORE, VIVO_GAME, VIVO_GAMEWATCH, HEYTAP_MARKET
    )

    /** Apps whose notifications we understand well enough to filter heuristically. */
    val KNOWN_PROVIDERS: Set<String> = MESSAGING + BROWSERS + APP_STORES +
        setOf(GMAIL, FACEBOOK)
}

/** A stored rule in its raw, un-compiled form. */
data class RuleSpec(
    val pattern: String,
    val type: RuleType,
    val action: RuleAction,
    val description: String = "",
    val packageScope: String = "*",
    /** True when the user wrote this rule by hand in the UI; false for bundled/community rules. */
    val isUserRule: Boolean = false,
)

/**
 * A rule compiled exactly once.
 *
 * [regex] is null when the stored pattern is not valid regex; such a rule can never match,
 * which is the safe failure mode (a broken rule must not take the whole engine down).
 */
data class CompiledRule(
    val pattern: String,
    val regex: Regex?,
    val type: RuleType,
    val action: RuleAction,
    val description: String,
    val targets: Set<String>,
    val isUserRule: Boolean = false,
) {
    /**
     * Whether this rule may fire for [packageName].
     *
     * An empty target set means "all packages". A rule scoped to specific packages never
     * fires for anything else, which is what stops a WhatsApp deal-spam rule from silencing
     * a personal chat or a bank alert.
     */
    fun appliesTo(packageName: String?): Boolean {
        if (packageName == null) return targets.isEmpty()
        return targets.isEmpty() || "*" in targets || packageName in targets
    }

    fun matches(text: String): Boolean = regex?.containsMatchIn(text) == true

    fun reason(fallback: String): String = description.ifBlank { fallback }
}

/** A single allowlist entry, decoupled from the Room entity. */
data class AllowEntry(val value: String, val type: AllowlistType)

/**
 * Everything the decision core is allowed to know about the outside world.
 *
 * Defaults are intentionally "protect nothing, block nothing": a caller that forgets to wire
 * up contacts degrades to permissive behaviour instead of silently eating messages.
 */
class PolicyEnvironment(
    val rules: List<CompiledRule> = emptyList(),
    val allowlist: List<AllowEntry> = emptyList(),
    val isPhoneContact: (String) -> Boolean = { false },
    val isNameContact: (String) -> Boolean = { false },
    val isEmergencyNumber: (String) -> Boolean = { false },
) {
    fun allowlistMatches(value: String, text: String? = null): Boolean {
        if (value.isBlank()) return false
        val needle = value.trim()
        return allowlist.any { entry ->
            val allowed = entry.value.trim()
            if (allowed.isEmpty()) return@any false
            when (entry.type) {
                AllowlistType.PHONE_NUMBER, AllowlistType.SENDER_HEADER ->
                    needle.equals(allowed, ignoreCase = true) ||
                        // Substring matching only for values long enough to be meaningful;
                        // a two-letter entry would otherwise match half the alphabet.
                        (allowed.length >= 3 && needle.contains(allowed, ignoreCase = true))
                AllowlistType.KEYWORD ->
                    text != null && text.contains(allowed, ignoreCase = true)
            }
        }
    }
}

/**
 * Stateless decision procedures: the pure decision core of ShieldGuard.
 *
 * This file deliberately has **no Android dependencies**. Everything it needs is passed in
 * through [PolicyEnvironment], which makes the whole filtering logic unit-testable on the JVM
 * without an emulator, a device, or Robolectric.
 *
 * [SpamEngine] is the thin Android adapter that loads rules/allowlist/contacts and calls in here.
 * Every branch is a pure function of its arguments, so the same input always produces the same
 * verdict.
 */
object SpamPolicy {

    // ---------------------------------------------------------------------------------------
    // Calls
    // ---------------------------------------------------------------------------------------

    fun decideCall(rawNumber: String, env: PolicyEnvironment): FilterDecision {
        val number = rawNumber.trim()
        if (number.isBlank()) return FilterDecision.Allow

        // Tier 1 — never block emergency services.
        if (env.isEmergencyNumber(number)) return FilterDecision.Allow

        // Tier 1 — saved contacts and explicit allowlists are untouchable.
        if (env.isPhoneContact(number)) return FilterDecision.Allow
        if (env.allowlistMatches(number)) return FilterDecision.Allow

        // ALLOW rules outrank BLOCK rules regardless of database insertion order.
        val callRules = env.rules.filter { it.type == RuleType.CALL_REGEX }
        callRules.firstOrNull { it.action == RuleAction.ALLOW && it.matches(number) }
            ?.let { return FilterDecision.Allow }

        callRules.firstOrNull { it.action != RuleAction.ALLOW && it.matches(number) }
            ?.let { rule -> return rule.toDecision("Matched pattern: ${rule.pattern}") }

        return FilterDecision.Allow
    }

    // ---------------------------------------------------------------------------------------
    // SMS
    // ---------------------------------------------------------------------------------------

    fun decideSms(sender: String, messageBody: String, env: PolicyEnvironment): FilterDecision {
        val from = sender.trim()
        val body = messageBody.trim()

        // Tier 1 — contacts, allowlisted senders, allowlisted keywords.
        if (from.isNotBlank() && env.isPhoneContact(from)) return FilterDecision.Allow
        if (env.allowlistMatches(from)) return FilterDecision.Allow
        if (env.allowlistMatches(from, body)) return FilterDecision.Allow

        // Tier 1 — OTPs and transactional bank messages are never dropped.
        if (OtpGuard.isOtpOrTransactional(body)) return FilterDecision.Allow

        val smsRules = env.rules.filter {
            it.type == RuleType.SMS_REGEX ||
                it.type == RuleType.SMS_KEYWORD ||
                it.type == RuleType.WHITELIST_REGEX
        }

        // Whitelist rules win over blocking rules, regardless of insertion order.
        smsRules.firstOrNull {
            (it.type == RuleType.WHITELIST_REGEX || it.action == RuleAction.ALLOW) && it.matches(body)
        }?.let { return FilterDecision.Allow }

        smsRules.firstOrNull {
            it.type != RuleType.WHITELIST_REGEX && it.action != RuleAction.ALLOW && it.matches(body)
        }?.let { rule -> return rule.toDecision("Matched SMS rule") }

        if (hasSuspiciousPatterns(body)) {
            return FilterDecision.Block("Heuristic: suspicious link in a scam-phrased message")
        }

        return FilterDecision.Allow
    }

    // ---------------------------------------------------------------------------------------
    // Notifications
    // ---------------------------------------------------------------------------------------

    /**
     * @param isGroupConversation true/false when the platform tells us, null when unknown.
     *        Used to guarantee that a one-to-one chat with a saved contact is never filtered.
     */
    fun decideNotification(
        packageName: String,
        title: String,
        text: String,
        env: PolicyEnvironment,
        isGroupConversation: Boolean? = null,
    ): FilterDecision {
        val fullContent = "$title $text".trim()
        if (fullContent.isBlank()) return FilterDecision.Allow

        // Tier 1 — 2FA / OTP / bank alerts survive every channel.
        if (OtpGuard.isOtpOrTransactional(fullContent)) return FilterDecision.Allow

        // Tier 1 — explicit allowlist rescue (allowlisted sender header or keyword).
        if (env.allowlistMatches(title, fullContent)) return FilterDecision.Allow

        val notificationRules = env.rules.filter {
            (it.type == RuleType.NOTIFICATION_KEYWORD || it.type == RuleType.NOTIFICATION_REGEX) &&
                it.appliesTo(packageName)
        }

        // Rules the user wrote by hand always outrank our own opinions, in both directions.
        val userRules = notificationRules.filter { it.isUserRule }
        userRules.firstOrNull { it.action == RuleAction.ALLOW && it.matches(fullContent) }
            ?.let { return FilterDecision.Allow }
        userRules.firstOrNull { it.action != RuleAction.ALLOW && it.matches(fullContent) }
            ?.let { rule -> return rule.toDecision("Matched your rule") }

        // Tier 1 — a 1:1 conversation with someone in your contacts is never a spam target.
        // This guard sits above the bundled rules on purpose: the shipped "deals|order now"
        // pattern must not be able to silence a friend writing about an actual order.
        if (packageName in KnownPackages.MESSAGING) {
            val namedContact = title.isNotBlank() && env.isNameContact(title.trim())
            if (namedContact && isGroupConversation != true) return FilterDecision.Allow
        }

        // Bundled and community rules.
        notificationRules.firstOrNull { it.action == RuleAction.ALLOW && it.matches(fullContent) }
            ?.let { return FilterDecision.Allow }
        notificationRules.firstOrNull { it.action != RuleAction.ALLOW && it.matches(fullContent) }
            ?.let { rule -> return rule.toDecision("Matched notification rule") }

        // Unknown app: we do not guess. Only explicit rules above may act.
        if (packageName !in KnownPackages.KNOWN_PROVIDERS) return FilterDecision.Allow

        when (packageName) {
            KnownPackages.WHATSAPP, KnownPackages.WHATSAPP_BUSINESS -> {
                if (WHATSAPP_PRODUCT_SPAM_REGEX.containsMatchIn(fullContent)) {
                    return FilterDecision.Block("WhatsApp product & deal spam")
                }
                if (hasPriceAndPromoPattern(fullContent)) {
                    return FilterDecision.Block("WhatsApp commercial price offer")
                }
            }

            KnownPackages.FACEBOOK -> {
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

            // Gmail is deliberately conservative: we only act on the one pattern the user asked
            // for. Guessing whether an email is "spam" risks eating a real one.
            KnownPackages.GMAIL -> {
                if (DISCORD_GMAIL_REGEX.containsMatchIn(fullContent)) {
                    return FilterDecision.Block("Discord mention via Gmail")
                }
            }
        }

        if (packageName in KnownPackages.BROWSERS) {
            val isDownloadNotification = fullContent.contains("download", ignoreCase = true) &&
                (fullContent.contains("complete", ignoreCase = true) ||
                    fullContent.contains("progress", ignoreCase = true) ||
                    fullContent.contains("finished", ignoreCase = true))
            if (!isDownloadNotification && BROWSER_CLICKBAIT_REGEX.containsMatchIn(fullContent)) {
                return FilterDecision.Block("Browser push spam")
            }
            if (!isDownloadNotification && hasPromotionalKeywords(fullContent)) {
                return FilterDecision.Block("Browser promotional spam")
            }
        }

        if (packageName in KnownPackages.APP_STORES) {
            val isActualInstall = fullContent.contains("installing", ignoreCase = true) &&
                !fullContent.contains("recommended", ignoreCase = true)
            if (!isActualInstall || APP_STORE_PROMO_REGEX.containsMatchIn(fullContent)) {
                return FilterDecision.Block("App Store promotional ad")
            }
        }

        return FilterDecision.Allow
    }

    // ---------------------------------------------------------------------------------------
    // Shared heuristics
    // ---------------------------------------------------------------------------------------

    private fun hasPriceAndPromoPattern(text: String): Boolean {
        val hasPrice = text.contains("₹") ||
            text.contains("rs.", ignoreCase = true) ||
            text.contains("inr", ignoreCase = true) ||
            text.contains("$")
        val hasOffer = text.contains("off", ignoreCase = true) ||
            text.contains("only", ignoreCase = true) ||
            text.contains("free", ignoreCase = true) ||
            text.contains("price", ignoreCase = true) ||
            text.contains("order", ignoreCase = true)
        return hasPrice && hasOffer
    }

    private fun hasSuspiciousPatterns(text: String): Boolean {
        val hasLink = text.contains("http://") ||
            text.contains("https://") ||
            text.contains("bit.ly") ||
            text.contains("tinyurl.com")
        val isScammy = text.contains("account suspended", ignoreCase = true) ||
            text.contains("click here to verify", ignoreCase = true) ||
            text.contains("lottery prize", ignoreCase = true) ||
            text.contains("claim bonus", ignoreCase = true)
        return hasLink && isScammy
    }

    private fun hasPromotionalKeywords(text: String): Boolean =
        GENERIC_PROMO_KEYWORDS.any { text.contains(it, ignoreCase = true) }

    // ---------------------------------------------------------------------------------------
    // Pattern library (unchanged intent, now scoped by caller)
    // ---------------------------------------------------------------------------------------

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

    private val DISCORD_GMAIL_REGEX = Regex(
        "(?i)\\b(discord)\\b.*\\b(" +
            "mentioned you|sent a message|new message|@everyone|@here|server|channel|" +
            "discordapp|notification@discord" +
            ")\\b"
    )

    private val BROWSER_CLICKBAIT_REGEX = Regex(
        "(?i)\\b(" +
            "breaking news|shocking|must watch|spin the wheel|win cash|daily horoscope|" +
            "viral video|trending news|click to read|exclusive reveal|top stories|" +
            "you won't believe|see what happened|latest gossip" +
            ")\\b"
    )

    private val APP_STORE_PROMO_REGEX = Regex(
        "(?i)\\b(" +
            "popular game|trending apps?|boost your phone|clean up|free gifts?|" +
            "coins? reward|daily check-in|must-have apps?|recommended apps?|" +
            "download now|install to earn|hot apps|update ready" +
            ")\\b"
    )

    private val GENERIC_PROMO_KEYWORDS = listOf(
        "50% off", "flat off", "use code", "buy 1 get 1", "exclusive discount",
        "free spin", "earn money daily", "claim your prize", "congratulations you won",
        "unlimited cashback", "scratch card", "instant loan", "zero interest loan"
    )
}

/** Converts a firing rule into a verdict, mapping SILENCE to its own decision type. */
private fun CompiledRule.toDecision(fallbackReason: String): FilterDecision = when (action) {
    RuleAction.SILENCE -> FilterDecision.Silence(reason(fallbackReason))
    RuleAction.ALLOW -> FilterDecision.Allow
    RuleAction.BLOCK -> FilterDecision.Block(reason(fallbackReason))
}

/** Compiles raw rule specs into matchers. Invalid regexes become inert rules. */
fun compileRules(specs: List<RuleSpec>): List<CompiledRule> = specs.map { spec ->
    val targets = spec.packageScope
        .split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()

    CompiledRule(
        pattern = spec.pattern,
        regex = runCatching { Regex(spec.pattern) }.getOrNull(),
        type = spec.type,
        action = spec.action,
        description = spec.description,
        targets = targets,
        isUserRule = spec.isUserRule,
    )
}
