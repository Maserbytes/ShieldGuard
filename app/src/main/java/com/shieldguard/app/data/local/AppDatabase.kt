package com.shieldguard.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shieldguard.app.data.local.dao.AllowlistDao
import com.shieldguard.app.data.local.dao.BlockLogDao
import com.shieldguard.app.data.local.dao.RuleDao
import com.shieldguard.app.data.local.entity.AllowlistEntity
import com.shieldguard.app.data.local.entity.BlockLogEntity
import com.shieldguard.app.data.local.entity.RuleAction
import com.shieldguard.app.data.local.entity.RuleEntity
import com.shieldguard.app.data.local.entity.RuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

@Database(
    entities = [RuleEntity::class, AllowlistEntity::class, BlockLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun allowlistDao(): AllowlistDao
    abstract fun blockLogDao(): BlockLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shield_guard.db"
                )
                    .addCallback(DatabasePrepopulateCallback(context.applicationContext))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabasePrepopulateCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                populateInitialRules(context)
            }
        }

        private suspend fun populateInitialRules(context: Context) {
            try {
                val jsonString = context.assets.open("default_rules.json").bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(jsonString)
                val rulesArray = jsonObject.getJSONArray("rules")
                val ruleList = mutableListOf<RuleEntity>()

                for (i in 0 until rulesArray.length()) {
                    val item = rulesArray.getJSONObject(i)
                    ruleList.add(
                        RuleEntity(
                            pattern = item.getString("pattern"),
                            type = RuleType.valueOf(item.getString("type")),
                            action = RuleAction.valueOf(item.getString("action")),
                            description = item.optString("description", ""),
                            isEnabled = item.optBoolean("enabled", true),
                            isUserCreated = false
                        )
                    )
                }

                getInstance(context).ruleDao().insertRules(ruleList)
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback default rules
                val fallbackRules = listOf(
                    RuleEntity(
                        pattern = "^(\\+?1?(800|888|877|866|855|844|833)\\d{7})$",
                        type = RuleType.CALL_REGEX,
                        action = RuleAction.BLOCK,
                        description = "Toll-Free Robocalls"
                    ),
                    RuleEntity(
                        pattern = "(?i)\\b(otp|verification code|one time password)\\b",
                        type = RuleType.WHITELIST_REGEX,
                        action = RuleAction.ALLOW,
                        description = "Universal OTP Protection"
                    )
                )
                getInstance(context).ruleDao().insertRules(fallbackRules)
            }
        }
    }
}
