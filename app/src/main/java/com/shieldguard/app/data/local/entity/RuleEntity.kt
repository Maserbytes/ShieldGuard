package com.shieldguard.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class RuleType {
    CALL_REGEX,
    SMS_KEYWORD,
    SMS_REGEX,
    NOTIFICATION_KEYWORD,
    NOTIFICATION_REGEX,
    WHITELIST_REGEX
}

enum class RuleAction {
    BLOCK,
    ALLOW,
    SILENCE
}

/**
 * A single filtering rule.
 *
 * The unique index on (pattern, type) is what keeps the weekly community rule sync from
 * accumulating an identical copy of every rule on every run.
 */
@Entity(
    tableName = "rules",
    indices = [Index(value = ["pattern", "type"], unique = true)]
)
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String,
    val type: RuleType,
    val action: RuleAction = RuleAction.BLOCK,
    val description: String = "",
    val isEnabled: Boolean = true,
    val isUserCreated: Boolean = false,
    /**
     * Comma-separated package names this rule may fire for, or `*` for every package.
     *
     * Scoping is what prevents a WhatsApp deal-spam rule from silencing an unrelated app's
     * notification (or a personal chat). Empty means "all packages" for backwards
     * compatibility with rules created before scoping existed.
     *
     * The `defaultValue` must match the ALTER TABLE in MIGRATION_2_3 exactly, otherwise Room's
     * schema validation rejects the migrated database on first open.
     */
    @ColumnInfo(defaultValue = "*")
    val packageScope: String = "*",
    val createdAt: Long = System.currentTimeMillis()
)
