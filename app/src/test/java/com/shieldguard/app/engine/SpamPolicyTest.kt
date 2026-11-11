package com.shieldguard.app.engine

import com.shieldguard.app.data.local.entity.AllowlistType
import com.shieldguard.app.data.local.entity.RuleAction
import com.shieldguard.app.data.local.entity.RuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavioural tests for the pure filtering core.
 *
 * These run on the JVM in milliseconds, with no emulator and no Android framework. They exist
 * because the previous "test suite" was a Python re-implementation of the Kotlin regexes: it
 * passed while the shipped engine had none of the package scoping or contact protections being
 * asserted here.
 */
class SpamPolicyTest {

    // -----------------------------------------------------------------------------------------
    // Test fixtures
    // -----------------------------------------------------------------------------------------

    private fun env(
        rules: List<RuleSpec> = emptyList(),
        allowlist: List<AllowEntry> = emptyList(),
        contacts: Set<String> = emptySet(),
        contactNames: Set<String> = emptySet(),
        emergency: Set<String> = setOf("911", "112", "999"),
    ): PolicyEnvironment {
        val loweredNames = contactNames.map { it.lowercase() }.toSet()
        return PolicyEnvironment(
            rules = compileRules(rules),
            allowlist = allowlist,
            isPhoneContact = { candidate -> candidate.trim() in contacts },
            isNameContact = { candidate -> candidate.trim().lowercase() in loweredNames },
            isEmergencyNumber = { candidate -> candidate.trim() in emergency },
        )
    }

    private fun rule(
        pattern: String,
        type: RuleType,
        action: RuleAction = RuleAction.BLOCK,
        scope: String = "*",
        isUserRule: Boolean = false,
    ) = RuleSpec(
        pattern = pattern,
        type = type,
        action = action,
        packageScope = scope,
        isUserRule = isUserRule,
    )

    /** The notification rules shipped in `assets/default_rules.json`. */
    private val bundledDealSpamRule = rule(
        pattern = "(?i)\\b(deals?|discounts?|order now|free delivery|coupon)\\b",
        type = RuleType.NOTIFICATION_KEYWORD,
        scope = "com.whatsapp,com.whatsapp.w4b",
    )

    // -----------------------------------------------------------------------------------------
    // Calls
    // -----------------------------------------------------------------------------------------

    @Test
    fun `emergency number is never blocked even when a rule matches it`() {
        val decision = SpamPolicy.decideCall(
            "911",
            env(rules = listOf(rule("^9\\d\\d$", RuleType.CALL_REGEX)))
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `saved contact is never blocked even when a rule matches it`() {
        val decision = SpamPolicy.decideCall(
            "+14155552671",
            env(
                rules = listOf(rule(".*", RuleType.CALL_REGEX)),
                contacts = setOf("+14155552671"),
            )
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `toll free number is blocked by the bundled rule`() {
        val decision = SpamPolicy.decideCall(
            "+18001234567",
            env(rules = listOf(rule("^(\\+?1?(800|888))\\d{7}$", RuleType.CALL_REGEX)))
        )
        assertTrue(decision is FilterDecision.Block)
    }

    @Test
    fun `allow rule beats block rule regardless of insertion order`() {
        val rules = listOf(
            rule(".*", RuleType.CALL_REGEX, RuleAction.BLOCK),
            rule("^\\+91140", RuleType.CALL_REGEX, RuleAction.ALLOW),
        )
        val decision = SpamPolicy.decideCall("+911401234567", env(rules = rules))
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `silence action yields a silence decision`() {
        val decision = SpamPolicy.decideCall(
            "+15550001111",
            env(rules = listOf(rule(".*", RuleType.CALL_REGEX, RuleAction.SILENCE)))
        )
        assertTrue(decision is FilterDecision.Silence)
    }

    @Test
    fun `unknown private caller is allowed`() {
        assertEquals(FilterDecision.Allow, SpamPolicy.decideCall("", env()))
    }

    // -----------------------------------------------------------------------------------------
    // SMS
    // -----------------------------------------------------------------------------------------

    @Test
    fun `bank otp is never blocked even when a keyword rule fires`() {
        val decision = SpamPolicy.decideSms(
            sender = "VM-HDFCBK",
            messageBody = "Your HDFC Bank verification code is 849201. Valid for 10 mins. Do not share.",
            env = env(
                rules = listOf(rule("(?i)verification code", RuleType.SMS_KEYWORD))
            ),
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `allowlisted sender header is never blocked`() {
        val decision = SpamPolicy.decideSms(
            sender = "AMAZON",
            messageBody = "Big deals available now on electronics",
            env = env(
                rules = listOf(rule("(?i)deals", RuleType.SMS_KEYWORD)),
                allowlist = listOf(AllowEntry("AMAZON", AllowlistType.SENDER_HEADER)),
            ),
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `whitelist rule beats a blocking rule added earlier`() {
        val rules = listOf(
            rule("(?i)welcome", RuleType.SMS_KEYWORD, RuleAction.BLOCK),
            rule("(?i)welcome bonus", RuleType.WHITELIST_REGEX, RuleAction.ALLOW),
        )
        val decision = SpamPolicy.decideSms("AD-BANK", "welcome bonus credited", env(rules))
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `phishing sms with link is blocked`() {
        val decision = SpamPolicy.decideSms(
            sender = "UNKNOWN",
            messageBody = "Your account suspended. Click here to verify http://evil.cc/verify",
            env = env(),
        )
        assertTrue(decision is FilterDecision.Block)
    }

    @Test
    fun `plain personal sms is allowed`() {
        val decision = SpamPolicy.decideSms(
            sender = "+919812345678",
            messageBody = "Hey, are we still meeting for lunch today at 1pm?",
            env = env(),
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `loan promotion sms is blocked by the keyword rule`() {
        val decision = SpamPolicy.decideSms(
            sender = "AD-LOAN",
            messageBody = "Your pre-approved loan of 50000 is ready, apply today",
            env = env(
                rules = listOf(rule("(?i)pre-approved loan", RuleType.SMS_KEYWORD))
            ),
        )
        assertTrue(decision is FilterDecision.Block)
    }

    // -----------------------------------------------------------------------------------------
    // Notifications — the false-positive regressions
    // -----------------------------------------------------------------------------------------

    @Test
    fun `one to one whatsapp chat with a saved contact survives deal keywords`() {
        val decision = SpamPolicy.decideNotification(
            packageName = KnownPackages.WHATSAPP,
            title = "Aarav Sharma",
            text = "I'll order now, free delivery is fine. Check the deals on that site.",
            env = env(
                rules = listOf(bundledDealSpamRule),
                contactNames = setOf("Aarav Sharma"),
            ),
            isGroupConversation = false,
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `whatsapp group deal spam is still blocked`() {
        val decision = SpamPolicy.decideNotification(
            packageName = KnownPackages.WHATSAPP,
            title = "Deals Group",
            text = "Lowest price wholesale! Order now, free delivery and COD available.",
            env = env(rules = listOf(bundledDealSpamRule)),
            isGroupConversation = true,
        )
        assertTrue(decision is FilterDecision.Block)
    }

    @Test
    fun `whatsapp group named after a contact is still filtered`() {
        // A group title can coincidentally equal a contact name; group flag must win.
        val decision = SpamPolicy.decideNotification(
            packageName = KnownPackages.WHATSAPP,
            title = "Aarav Sharma",
            text = "Big billion sale, use coupon code today, order now",
            env = env(
                rules = listOf(bundledDealSpamRule),
                contactNames = setOf("Aarav Sharma"),
            ),
            isGroupConversation = true,
        )
        assertTrue(decision is FilterDecision.Block)
    }

    @Test
    fun `bundled rule scoped to whatsapp does not fire on gmail`() {
        val decision = SpamPolicy.decideNotification(
            packageName = KnownPackages.GMAIL,
            title = "Nike",
            text = "New arrivals: order now and get free delivery on all orders.",
            env = env(rules = listOf(bundledDealSpamRule)),
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `personal gmail is not filtered by promo wording`() {
        val decision = SpamPolicy.decideNotification(
            packageName = KnownPackages.GMAIL,
            title = "Priya",
            text = "Can you order now? I'll pay you back, the deals end tonight.",
            env = env(rules = listOf(bundledDealSpamRule)),
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `unknown app is never blocked heuristically`() {
        val decision = SpamPolicy.decideNotification(
            packageName = "com.somebank.app",
            title = "ShieldGuard test",
            text = "Trending apps, download now, free gifts and coins reward",
            env = env(),
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `otp delivered in a notification is never blocked`() {
        val decision = SpamPolicy.decideNotification(
            packageName = "com.somebank.app",
            title = "Bank",
            text = "Use code 482913 to log in. Do not share this OTP.",
            env = env(rules = listOf(rule("(?i)use code", RuleType.NOTIFICATION_KEYWORD))),
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `discord mention in gmail is blocked`() {
        val decision = SpamPolicy.decideNotification(
            packageName = KnownPackages.GMAIL,
            title = "Discord",
            text = "@everyone Bob mentioned you in #crypto-chat",
            env = env(),
        )
        assertTrue(decision is FilterDecision.Block)
    }

    @Test
    fun `browser clickbait is blocked`() {
        val decision = SpamPolicy.decideNotification(
            packageName = KnownPackages.VIVO_BROWSER,
            title = "Vivo Browser",
            text = "Breaking news! Shocking viral video, click to read immediately",
            env = env(),
        )
        assertTrue(decision is FilterDecision.Block)
    }

    @Test
    fun `browser download completion is allowed`() {
        val decision = SpamPolicy.decideNotification(
            packageName = KnownPackages.VIVO_BROWSER,
            title = "Download complete",
            text = "report.pdf download finished",
            env = env(),
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `app store promo is blocked but a real install is allowed`() {
        val promo = SpamPolicy.decideNotification(
            packageName = KnownPackages.VIVO_APPSTORE,
            title = "V-Appstore",
            text = "Must-have apps! Install trending apps now and get coins reward",
            env = env(),
        )
        assertTrue(promo is FilterDecision.Block)

        val install = SpamPolicy.decideNotification(
            packageName = KnownPackages.VIVO_APPSTORE,
            title = "Installing",
            text = "Installing WhatsApp Business",
            env = env(),
        )
        assertEquals(FilterDecision.Allow, install)
    }

    @Test
    fun `a user rule outranks the contact guard`() {
        val decision = SpamPolicy.decideNotification(
            packageName = KnownPackages.WHATSAPP,
            title = "Aarav Sharma",
            text = "send me crypto",
            env = env(
                rules = listOf(
                    rule("(?i)crypto", RuleType.NOTIFICATION_KEYWORD, isUserRule = true)
                ),
                contactNames = setOf("Aarav Sharma"),
            ),
            isGroupConversation = false,
        )
        assertTrue(decision is FilterDecision.Block)
    }

    @Test
    fun `rule aimed at one package can list several`() {
        val scoped = rule(
            pattern = "(?i)promo",
            type = RuleType.NOTIFICATION_KEYWORD,
            scope = "com.a.app, com.b.app",
        )
        assertEquals(
            FilterDecision.Allow,
            SpamPolicy.decideNotification("com.c.app", "x", "promo", env(rules = listOf(scoped)))
        )
        // com.b.app is not a known provider, so only the explicit rule can act - and it does.
        assertTrue(
            SpamPolicy.decideNotification("com.b.app", "x", "promo", env(rules = listOf(scoped)))
                is FilterDecision.Block
        )
    }

    @Test
    fun `invalid regex never crashes the engine`() {
        val broken = rule("([unclosed", RuleType.NOTIFICATION_KEYWORD)
        val decision = SpamPolicy.decideNotification(
            packageName = KnownPackages.WHATSAPP,
            title = "Aarav",
            text = "hello",
            env = env(rules = listOf(broken)),
        )
        assertEquals(FilterDecision.Allow, decision)
    }

    @Test
    fun `allowlisted keyword rescues a message before any rule runs`() {
        val decision = SpamPolicy.decideNotification(
            packageName = KnownPackages.WHATSAPP,
            title = "Family",
            text = "school fee receipt attached, order now for uniform",
            env = env(
                rules = listOf(bundledDealSpamRule),
                allowlist = listOf(AllowEntry("school fee", AllowlistType.KEYWORD)),
            ),
            isGroupConversation = true,
        )
        assertEquals(FilterDecision.Allow, decision)
    }
}
