package com.liezert.itungen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.liezert.itungen.data.local.entity.RecurringRuleEntity
import com.liezert.itungen.data.local.entity.RecurringRuleWithCategory
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface RecurringRuleDao {
    @Insert
    suspend fun insert(rule: RecurringRuleEntity): Long

    @Query("SELECT * FROM recurring_rules WHERE is_active = 1")
    suspend fun getActive(): List<RecurringRuleEntity>

    @Query("UPDATE recurring_rules SET last_generated_date = :date WHERE id = :id")
    suspend fun setLastGenerated(id: Long, date: LocalDate)

    /** Aktif dulu, lalu urut tanggal jatuh tempo. */
    @Transaction
    @Query("SELECT * FROM recurring_rules ORDER BY is_active DESC, day_of_month, id")
    fun observeAll(): Flow<List<RecurringRuleWithCategory>>

    @Query("UPDATE recurring_rules SET is_active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    /** Menandai jatuh tempo sampai [date] sudah lewat, tanpa mundur bila sudah lebih baru. */
    @Query(
        """
        UPDATE recurring_rules SET last_generated_date = :date
        WHERE id = :id AND (last_generated_date IS NULL OR last_generated_date < :date)
        """
    )
    suspend fun skipUntil(id: Long, date: LocalDate)

    /** Transaksi yang sudah tercatat tetap ada (riwayat uang nyata); hanya pencatatan berikutnya yang berhenti. */
    @Query("DELETE FROM recurring_rules WHERE id = :id")
    suspend fun delete(id: Long)
}
