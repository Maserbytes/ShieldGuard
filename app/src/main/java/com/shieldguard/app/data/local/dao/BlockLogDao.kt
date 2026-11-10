package com.shieldguard.app.data.local.dao

import androidx.room.*
import com.shieldguard.app.data.local.entity.BlockLogEntity
import com.shieldguard.app.data.local.entity.CommunicationChannel
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockLogDao {
    @Query("SELECT * FROM block_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<BlockLogEntity>>

    @Query("SELECT * FROM block_logs WHERE channel = :channel ORDER BY timestamp DESC")
    fun getLogsByChannel(channel: CommunicationChannel): Flow<List<BlockLogEntity>>

    @Query("SELECT COUNT(*) FROM block_logs")
    fun getTotalBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM block_logs WHERE channel = :channel")
    fun getBlockedCountByChannel(channel: CommunicationChannel): Flow<Int>

    @Insert
    suspend fun insertLog(log: BlockLogEntity): Long

    @Query("DELETE FROM block_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM block_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)
}
