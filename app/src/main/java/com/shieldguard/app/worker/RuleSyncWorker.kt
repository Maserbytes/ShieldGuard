package com.shieldguard.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shieldguard.app.data.local.AppDatabase
import com.shieldguard.app.data.local.entity.RuleAction
import com.shieldguard.app.data.local.entity.RuleEntity
import com.shieldguard.app.data.local.entity.RuleType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class RuleSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "rule_sync_work"

        /** Free, public, zero-cost rule distribution via GitHub raw. */
        private const val DEFAULT_COMMUNITY_RULES_URL =
            "https://raw.githubusercontent.com/ShieldGuardApp/community-spam-rules/main/default_rules.json"

        /**
         * Types the community feed is allowed to create.
         *
         * CALL_REGEX is excluded on purpose: a hostile or mistaken pattern there could stop the
         * phone ringing for real callers, with no notification the user would ever see. Call
         * blocking stays a local, user-controlled decision.
         */
        private val REMOTE_ALLOWED_TYPES = setOf(
            RuleType.SMS_KEYWORD,
            RuleType.SMS_REGEX,
            RuleType.NOTIFICATION_KEYWORD,
            RuleType.NOTIFICATION_REGEX,
            RuleType.WHITELIST_REGEX,
        )

        private const val MAX_REMOTE_RULES = 500

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED) // Free Wi-Fi only
                .setRequiresBatteryNotLow(true)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<RuleSyncWorker>(
                repeatInterval = 7,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(DEFAULT_COMMUNITY_RULES_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ShieldGuard-Android/1.0")
            }

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext Result.retry()
            }

            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            val rules = parseRules(jsonText)
            if (rules.isEmpty()) return@withContext Result.success()

            AppDatabase.getInstance(applicationContext).ruleDao().insertRules(rules)
            Result.success()
        } catch (e: Exception) {
            // Offline or malformed feed: retry later without disturbing the user.
            Result.retry()
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Parses the remote feed defensively. One malformed entry must not discard the whole batch,
     * and every entry is clamped to a known type and a real package scope.
     */
    private fun parseRules(jsonText: String): List<RuleEntity> {
        val array = runCatching { JSONObject(jsonText).getJSONArray("rules") }.getOrNull()
            ?: return emptyList()

        val rules = mutableListOf<RuleEntity>()
        for (i in 0 until minOf(array.length(), MAX_REMOTE_RULES)) {
            val item = runCatching { array.getJSONObject(i) }.getOrNull() ?: continue
            val type = runCatching { RuleType.valueOf(item.getString("type")) }.getOrNull() ?: continue
            if (type !in REMOTE_ALLOWED_TYPES) continue

            val pattern = runCatching { item.getString("pattern") }.getOrNull()
            if (pattern.isNullOrBlank()) continue

            rules.add(
                RuleEntity(
                    pattern = pattern,
                    type = type,
                    action = runCatching { RuleAction.valueOf(item.getString("action")) }
                        .getOrDefault(RuleAction.BLOCK),
                    description = item.optString("description", "Community rule"),
                    isEnabled = item.optBoolean("enabled", true),
                    isUserCreated = false,
                    // Remote rules MUST declare their scope. Defaulting to "*" here is what
                    // previously let a WhatsApp rule silence unrelated apps.
                    packageScope = item.optString("packageScope", ""),
                )
            )
        }
        return rules.filter { it.packageScope.isNotBlank() }
    }
}
