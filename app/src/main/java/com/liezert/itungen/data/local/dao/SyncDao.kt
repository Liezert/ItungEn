package com.liezert.itungen.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.liezert.itungen.data.local.entity.DeletedTransactionEntity
import com.liezert.itungen.data.local.entity.GoalEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.local.entity.WalletEntity
import com.liezert.itungen.domain.model.GoalStatus
import kotlinx.coroutines.flow.Flow

/** Transaksi yang belum terunggah, dengan uuid dompet & kategorinya (id lokal tidak berarti di HP lain). */
data class PendingTransaction(
    @Embedded val tx: TransactionEntity,
    @ColumnInfo(name = "wallet_uuid") val walletUuid: String,
    @ColumnInfo(name = "category_uuid") val categoryUuid: String?,
)

/** Goal yang belum terunggah. Dompet GOAL-nya ikut di dokumen goal yang sama (1:1). */
data class PendingGoal(
    @Embedded val goal: GoalEntity,
    @ColumnInfo(name = "wallet_uuid") val walletUuid: String,
    @ColumnInfo(name = "wallet_archived") val walletArchived: Boolean,
)

private const val PendingCount =
    "SELECT (SELECT COUNT(*) FROM transactions WHERE synced = 0) + (SELECT COUNT(*) FROM goals WHERE synced = 0) + " +
        "(SELECT COUNT(*) FROM deleted_transactions WHERE pushed = 0)"

@Dao
interface SyncDao {
    @Query(PendingCount)
    fun observePendingCount(): Flow<Int>

    @Query(PendingCount)
    suspend fun pendingCount(): Int

    @Query(
        """
        SELECT g.*, w.uuid AS wallet_uuid, w.is_archived AS wallet_archived
        FROM goals g JOIN wallets w ON w.id = g.wallet_id WHERE g.synced = 0
        """
    )
    suspend fun pendingGoals(): List<PendingGoal>

    /** Per 400 baris: satu batch Firestore maksimal 500 tulisan. */
    @Query(
        """
        SELECT t.*, w.uuid AS wallet_uuid, c.uuid AS category_uuid
        FROM transactions t JOIN wallets w ON w.id = t.wallet_id LEFT JOIN categories c ON c.id = t.category_id
        WHERE t.synced = 0 ORDER BY t.id LIMIT 400
        """
    )
    suspend fun pendingTransactions(): List<PendingTransaction>

    // Cadangan (ekspor): semua baris, bentuk sama dengan dokumen sinkron.
    @Query(
        """
        SELECT g.*, w.uuid AS wallet_uuid, w.is_archived AS wallet_archived
        FROM goals g JOIN wallets w ON w.id = g.wallet_id ORDER BY g.id
        """
    )
    suspend fun allGoals(): List<PendingGoal>

    @Query(
        """
        SELECT t.*, w.uuid AS wallet_uuid, c.uuid AS category_uuid
        FROM transactions t JOIN wallets w ON w.id = t.wallet_id LEFT JOIN categories c ON c.id = t.category_id
        ORDER BY t.date, t.id
        """
    )
    suspend fun allTransactions(): List<PendingTransaction>

    @Query("SELECT uuid FROM deleted_transactions WHERE pushed = 0 LIMIT 400")
    suspend fun pendingDeletes(): List<String>

    @Query("UPDATE deleted_transactions SET pushed = 1 WHERE uuid IN (:uuids)")
    suspend fun markDeletesSynced(uuids: List<String>)

    /** Semua uuid yang pernah dihapus (cadangan dan pemulihan melewatinya). */
    @Query("SELECT uuid FROM deleted_transactions")
    suspend fun deletedUuids(): List<String>

    /** Penanda dari HP lain (sudah ada di server) atau dari cadangan. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTombstone(tombstone: DeletedTransactionEntity)

    @Query("DELETE FROM deleted_transactions WHERE uuid = :uuid")
    suspend fun deleteTombstone(uuid: String)

    /** Transaksi dihapus di HP lain. */
    @Query("DELETE FROM transactions WHERE uuid = :uuid")
    suspend fun deleteTransaction(uuid: String)

    @Query("UPDATE transactions SET synced = 1 WHERE uuid IN (:uuids)")
    suspend fun markTransactionsSynced(uuids: List<String>)

    /** Status ikut dicek: bila goal berubah lagi selama unggah, ia tetap tertandai belum tersinkron. */
    @Query("UPDATE goals SET synced = 1 WHERE uuid = :uuid AND status = :status")
    suspend fun markGoalSynced(uuid: String, status: GoalStatus)

    @Query("SELECT id FROM wallets WHERE uuid = :uuid")
    suspend fun walletId(uuid: String): Long?

    @Query("SELECT id FROM categories WHERE uuid = :uuid")
    suspend fun categoryId(uuid: String): Long?

    @Query("SELECT * FROM goals WHERE uuid = :uuid")
    suspend fun goal(uuid: String): GoalEntity?

    /** Transaksi tidak pernah diubah setelah dicatat, jadi uuid yang sudah ada cukup dilewati. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert
    suspend fun insertWallet(wallet: WalletEntity): Long

    @Insert
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("UPDATE wallets SET is_archived = :archived WHERE id = :id")
    suspend fun setWalletArchived(id: Long, archived: Boolean)

    // Keluar akun: data milik akun dihapus dari HP; dompet utama dan kategori bawaan tetap (urutan mengikuti FK).
    @Query("DELETE FROM transactions")
    suspend fun deleteTransactions()

    @Query("DELETE FROM deleted_transactions")
    suspend fun deleteTombstones()

    @Query("DELETE FROM goals")
    suspend fun deleteGoals()

    @Query("DELETE FROM wallets WHERE type != 'MAIN'")
    suspend fun deleteGoalWallets()
}
