package com.shieldguard.app.engine

import android.content.Context
import com.shieldguard.app.data.repository.ShieldRepository

/**
 * Android adapter around [SpamPolicy].
 *
 * Its only jobs are: load rules, allowlist and contacts from the device, compile the rules once,
 * and hand the pure decision core everything it needs. All the actual filtering logic lives in
 * [SpamPolicy] so it can be tested on the JVM.
 */
class SpamEngine(context: Context) {

    private val appContext = context.applicationContext
    private val repository = ShieldRepository(appContext)

    @Volatile
    private var cachedRules: List<CompiledRule>? = null

    @Volatile
    private var cachedRulesAt: Long = 0L

    /**
     * Rules change at most a handful of times per day (user edit or weekly sync), but they are
     * read on every single notification. Compiling the regexes once and reusing them for a few
     * seconds keeps the hot path free of both DB reads and regex compilation.
     */
    private suspend fun compiledRules(): List<CompiledRule> {
        val now = System.currentTimeMillis()
        cachedRules?.let { if (now - cachedRulesAt < RULE_CACHE_TTL_MS) return it }

        val specs = repository.getActiveRules().map { rule ->
            RuleSpec(
                pattern = rule.pattern,
                type = rule.type,
                action = rule.action,
                description = rule.description,
                packageScope = rule.packageScope,
                isUserRule = rule.isUserCreated,
            )
        }
        val compiled = compileRules(specs)
        cachedRules = compiled
        cachedRulesAt = now
        return compiled
    }

    private suspend fun environment(
        phoneNumber: String? = null,
        displayName: String? = null,
    ): PolicyEnvironment {
        val rules = compiledRules()
        val allowlist = repository.getAllowlist().map { AllowEntry(it.value, it.type) }

        val normalizedNumber = phoneNumber?.trim()
        val numberIsContact =
            !normalizedNumber.isNullOrBlank() && repository.isContact(normalizedNumber)

        val normalizedName = displayName?.trim()
        val nameIsContact =
            !normalizedName.isNullOrBlank() && repository.isContactByName(normalizedName)

        return PolicyEnvironment(
            rules = rules,
            allowlist = allowlist,
            isPhoneContact = { candidate ->
                numberIsContact && candidate.trim() == normalizedNumber
            },
            isNameContact = { candidate ->
                nameIsContact && candidate.trim().equals(normalizedName, ignoreCase = true)
            },
            isEmergencyNumber = { candidate -> EmergencyGuard.isEmergency(appContext, candidate) },
        )
    }

    suspend fun evaluateCall(rawNumber: String): FilterDecision =
        SpamPolicy.decideCall(rawNumber, environment(phoneNumber = rawNumber))

    suspend fun evaluateSms(sender: String, messageBody: String): FilterDecision =
        SpamPolicy.decideSms(sender, messageBody, environment(phoneNumber = sender))

    suspend fun evaluateNotification(
        packageName: String,
        title: String,
        text: String,
        isGroupConversation: Boolean? = null,
    ): FilterDecision = SpamPolicy.decideNotification(
        packageName = packageName,
        title = title,
        text = text,
        env = environment(displayName = title),
        isGroupConversation = isGroupConversation,
    )

    private companion object {
        const val RULE_CACHE_TTL_MS = 5_000L
    }
}
