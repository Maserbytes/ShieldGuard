package com.shieldguard.app.data.local.entity

import androidx.room.Entity
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

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val pattern: String,
    val type: RuleType,
    val action: RuleAction = RuleAction.BLOCK,
    val description: String = "",
    val isEnabled: Boolean = true,
    val isUserCreated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
