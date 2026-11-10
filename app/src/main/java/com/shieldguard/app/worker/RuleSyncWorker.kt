package com.shieldguard.app.worker

import android.content.Context
import androidx.work.*
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
        // Free public GitHub raw repository or Gist for community rules ($0 hosting)
        private const val DEFAULT_COMMUNITY_RULES_URL =
            "https://raw.githubusercontent.com/ShieldGuardApp/community-spam-rules/main/default_rules.json"

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
        try {
            val url = URL(DEFAULT_COMMUNITY_RULES_URL)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 15_000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "ShieldGuard-Android/1.0")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(jsonText)
                val rulesArray = jsonObject.getJSONArray("rules")
                val ruleList = mutableListOf<RuleEntity>()

                for (i in 0 until rulesArray.length()) {
                    val item = rulesArray.getJSONObject(i)
                    ruleList.add(
                        RuleEntity(
                            pattern = item.getString("pattern"),
                            type = RuleType.valueOf(item.getString("type")),
                            action = RuleAction.valueOf(item.getString("action")),
                            description = item.optString("description", "Community Rule"),
                            isEnabled = item.optBoolean("enabled", true),
                            isUserCreated = false
                        )
                    )
                }

                if (ruleList.isNotEmpty()) {
                    val db = AppDatabase.getInstance(applicationContext)
                    db.ruleDao().insertRules(ruleList)
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            // Offline or network error: gracefully retry without disturbing the user
            Result.retry()
        }
    }
}
