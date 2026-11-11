package com.shieldguard.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shieldguard.app.data.local.dao.AllowlistDao
import com.shieldguard.app.data.local.dao.BlockLogDao
import com.shieldguard.app.data.local.dao.RuleDao
import com.shieldguard.app.data.local.entity.AllowlistEntity
import com.shieldguard.app.data.local.entity.BlockLogEntity
import com.shieldguard.app.data.local.entity.RuleAction
import com.shieldguard.app.data.local.entity.RuleEntity
import com.shieldguard.app.data.local.entity.RuleType
import org.json.JSONObject

@Database(
    entities = [RuleEntity::class, AllowlistEntity::class, BlockLogEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun allowlistDao(): AllowlistDao
    abstract fun blockLogDao(): BlockLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v2 -> v3: rules gain an optional package scope. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE rules ADD COLUMN packageScope TEXT NOT NULL DEFAULT '*'"
                )

                // Retro-fit scopes onto the bundled rules so existing installs benefit from the
                // false-positive fix immediately. Only non-user rules are touched.
                val retarget = listOf(
                    "WhatsApp Group Product & Deals Spam" to
                        "com.whatsapp,com.whatsapp.w4b",
                    "Facebook Friend Suggestions & Congrats Spam" to
                        "com.facebook.katana",
                    "Discord Mentions via Gmail" to
                        "com.google.android.gm",
                    "Browser & System News Clickbait" to
                        "com.vivo.browser,com.vivo.hybrid,com.android.chrome,com.heytap.browser,com.coloros.browser,com.opera.browser",
                    "App Store & Bloatware Push Ads" to
                        "com.vivo.appstore,com.bbk.appstore,com.vivo.game,com.vivo.gamewatch,com.heytap.market",
                )
                for ((description, scope) in retarget) {
                    db.execSQL(
                        "UPDATE rules SET packageScope = ? WHERE description = ? AND isUserCreated = 0",
                        arrayOf(scope, description)
                    )
                }

                // v2 could accumulate duplicates because the sync used INSERT OR IGNORE with an
                // auto-generated primary key. Collapse them before enforcing uniqueness.
                db.execSQL(
                    "DELETE FROM rules WHERE id NOT IN " +
                        "(SELECT MIN(id) FROM rules GROUP BY pattern, type)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_rules_pattern_type ON rules (pattern, type)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shield_guard.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigrationFrom(1)
                    .addCallback(DatabasePrepopulateCallback(context.applicationContext))
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    /**
     * Seeds the bundled rules on first creation.
     *
     * This runs synchronously inside Room's `onCreate`, using raw SQL rather than the DAO.
     * That matters: calling back into [getInstance] or a DAO from this callback re-enters the
     * database while it is still opening, which can deadlock, and launching it on a background
     * coroutine leaves a window after boot where no rules exist at all.
     */
    private class DatabasePrepopulateCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            for (rule in loadBundledRules()) {
                db.execSQL(
                    "INSERT INTO rules " +
                        "(pattern, type, action, description, isEnabled, isUserCreated, packageScope, createdAt) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(
                        rule.pattern,
                        // Room stores enums by name; binding the enum object itself would throw
                        // "Cannot bind class" at runtime.
                        rule.type.name,
                        rule.action.name,
                        rule.description,
                        if (rule.isEnabled) 1 else 0,
                        if (rule.isUserCreated) 1 else 0,
                        rule.packageScope,
                        rule.createdAt,
                    )
                )
            }
        }

        private fun loadBundledRules(): List<RuleEntity> = runCatching {
            val json = context.assets.open("default_rules.json")
                .bufferedReader()
                .use { it.readText() }
            val array = JSONObject(json).getJSONArray("rules")
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        RuleEntity(
                            pattern = item.getString("pattern"),
                            type = RuleType.valueOf(item.getString("type")),
                            action = RuleAction.valueOf(item.getString("action")),
                            description = item.optString("description", ""),
                            isEnabled = item.optBoolean("enabled", true),
                            isUserCreated = false,
                            packageScope = item.optString("packageScope", "*"),
                        )
                    )
                }
            }
        }.getOrElse {
            // The asset is bundled with the APK, so this should be unreachable. If it does
            // happen, ship a minimal protective set rather than an empty rule table.
            listOf(
                RuleEntity(
                    pattern = "^(\\+?1?(800|888|877|866|855|844|833)\\d{7})$",
                    type = RuleType.CALL_REGEX,
                    action = RuleAction.BLOCK,
                    description = "Toll-Free Robocalls",
                ),
            )
        }
    }
}
