package com.liezert.itungen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.liezert.itungen.data.local.entity.GoalEntity
import com.liezert.itungen.data.local.entity.GoalWithSaved
import com.liezert.itungen.domain.model.GoalStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun get(id: Long): GoalEntity

    /** Goal aktif dan tercapai (yang dibatalkan disembunyikan), aktif dulu, beserta dana terkumpulnya. */
    @Query(
        """
        SELECT g.*, COALESCE((
            SELECT SUM(CASE WHEN t.type IN ('INCOME', 'TRANSFER_IN') THEN t.amount ELSE -t.amount END)
            FROM transactions t WHERE t.wallet_id = g.wallet_id
        ), 0) AS saved
        FROM goals g
        WHERE g.status != 'CANCELLED'
        ORDER BY g.status = 'ACHIEVED', g.id
        """
    )
    fun observeWithSaved(): Flow<List<GoalWithSaved>>

    // synced = 0: status baru (tercapai / dibatalkan) ikut terunggah.
    @Query("UPDATE goals SET status = :status, synced = 0 WHERE id = :id")
    suspend fun setStatus(id: Long, status: GoalStatus)
}
