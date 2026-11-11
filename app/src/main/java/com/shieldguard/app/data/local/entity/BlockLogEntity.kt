package com.shieldguard.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CommunicationChannel {
    CALL,
    SMS,
    WHATSAPP,
    GMAIL,
    FACEBOOK,
    BROWSER,
    APP_STORE,
    OTHER
}

@Entity(tableName = "block_logs")
data class BlockLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val channel: CommunicationChannel,
    val senderOrCaller: String,
    val contentSnippet: String,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
