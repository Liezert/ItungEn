package com.liezert.itungen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.local.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    /** -1 bila baris sudah ada (unique index recurring_rule_id + date), jadi aman dijalankan berulang. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(transaction: TransactionEntity): Long

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC, id DESC")
    fun observeAll(): Flow<List<TransactionWithCategory>>

    /** Saldo = masuk (INCOME, TRANSFER_IN) − keluar (EXPENSE, TRANSFER_OUT). Satu sumber kebenaran (§3.2). */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type IN ('INCOME', 'TRANSFER_IN') THEN amount ELSE -amount END), 0)
        FROM transactions WHERE wallet_id = :walletId
        """
    )
    fun observeBalance(walletId: Long): Flow<Long>
}
