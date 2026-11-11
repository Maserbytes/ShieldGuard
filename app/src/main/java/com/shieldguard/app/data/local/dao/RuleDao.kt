package com.shieldguard.app.data.local.dao

import androidx.room.*
import com.shieldguard.app.data.local.entity.RuleEntity
import com.shieldguard.app.data.local.entity.RuleType
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<RuleEntity>

    @Query("SELECT * FROM rules WHERE isEnabled = 1 AND type = :type")
    suspend fun getActiveRulesByType(type: RuleType): List<RuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<RuleEntity>)

    @Update
    suspend fun updateRule(rule: RuleEntity)

    @Delete
    suspend fun deleteRule(rule: RuleEntity)

    @Query("DELETE FROM rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: Long)
}
