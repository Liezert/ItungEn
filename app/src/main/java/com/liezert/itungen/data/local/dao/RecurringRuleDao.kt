package com.liezert.itungen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.liezert.itungen.data.local.entity.RecurringRuleEntity
import java.time.LocalDate

@Dao
interface RecurringRuleDao {
    @Insert
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Query("SELECT * FROM recurring_rules WHERE is_active = 1")
    suspend fun getActive(): List<RecurringRuleEntity>

    @Query("UPDATE recurring_rules SET last_generated_date = :date WHERE id = :id")
    suspend fun setLastGenerated(id: Long, date: LocalDate)
}
