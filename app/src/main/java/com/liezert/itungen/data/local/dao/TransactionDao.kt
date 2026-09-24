package com.liezert.itungen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.liezert.itungen.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    /** Saldo = masuk (INCOME, TRANSFER_IN) − keluar (EXPENSE, TRANSFER_OUT). Satu sumber kebenaran (§3.2). */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type IN ('INCOME', 'TRANSFER_IN') THEN amount ELSE -amount END), 0)
        FROM transactions WHERE wallet_id = :walletId
        """
    )
    fun observeBalance(walletId: Long): Flow<Long>
}
