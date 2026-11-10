package com.shieldguard.app.data.local.dao

import androidx.room.*
import com.shieldguard.app.data.local.entity.AllowlistEntity
import com.shieldguard.app.data.local.entity.AllowlistType
import kotlinx.coroutines.flow.Flow

@Dao
interface AllowlistDao {
    @Query("SELECT * FROM allowlist ORDER BY id DESC")
    fun getAllAllowlist(): Flow<List<AllowlistEntity>>

    @Query("SELECT * FROM allowlist")
    suspend fun getActiveAllowlist(): List<AllowlistEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM allowlist WHERE value = :value AND type = :type)")
    suspend fun isAllowed(value: String, type: AllowlistType): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AllowlistEntity): Long

    @Delete
    suspend fun delete(entry: AllowlistEntity)

    @Query("DELETE FROM allowlist WHERE id = :id")
    suspend fun deleteById(id: Long)
}
