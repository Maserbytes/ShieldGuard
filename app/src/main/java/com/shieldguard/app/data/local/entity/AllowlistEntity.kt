package com.shieldguard.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AllowlistType {
    PHONE_NUMBER,
    SENDER_HEADER, // e.g. "HDFCBK", "AMAZON"
    KEYWORD
}

@Entity(tableName = "allowlist")
data class AllowlistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val value: String,
    val type: AllowlistType,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
