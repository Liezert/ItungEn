package com.liezert.itungen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.local.entity.TransactionWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC, id DESC")
    fun observeAll(): Flow<List<TransactionWithCategory>>

    /** Riwayat satu dompet; Beranda dan Semua Transaksi hanya menampilkan dompet utama. */
    @Transaction
    @Query("SELECT * FROM transactions WHERE wallet_id = :walletId ORDER BY date DESC, created_at DESC, id DESC")
    fun observeForWallet(walletId: Long): Flow<List<TransactionWithCategory>>

    /** Saldo = masuk (INCOME, TRANSFER_IN) − keluar (EXPENSE, TRANSFER_OUT). Satu sumber kebenaran (§3.2). */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type IN ('INCOME', 'TRANSFER_IN') THEN amount ELSE -amount END), 0)
        FROM transactions WHERE wallet_id = :walletId
        """
    )
    fun observeBalance(walletId: Long): Flow<Long>

    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type IN ('INCOME', 'TRANSFER_IN') THEN amount ELSE -amount END), 0)
        FROM transactions WHERE wallet_id = :walletId
        """
    )
    suspend fun balance(walletId: Long): Long
}
