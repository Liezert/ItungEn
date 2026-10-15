package com.liezert.itungen.data.local.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.liezert.itungen.data.local.entity.CategoryEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.local.entity.TransactionWithCategory
import com.liezert.itungen.domain.model.TransactionType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): TransactionEntity

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)

    @Transaction
    @Query("SELECT * FROM transactions ORDER BY date DESC, created_at DESC, id DESC")
    fun observeAll(): Flow<List<TransactionWithCategory>>

    /** Seluruh riwayat satu dompet (ekspor CSV); layar memakai kueri berjendela di bawah. */
    @Transaction
    @Query("SELECT * FROM transactions WHERE wallet_id = :walletId ORDER BY date DESC, created_at DESC, id DESC")
    suspend fun allForWallet(walletId: Long): List<TransactionWithCategory>

    /** [limit] transaksi terbaru (Beranda). */
    @Transaction
    @Query("SELECT * FROM transactions WHERE wallet_id = :walletId ORDER BY date DESC, created_at DESC, id DESC LIMIT :limit")
    fun observeRecent(walletId: Long, limit: Int): Flow<List<TransactionWithCategory>>

    /** Jendela tanggal [from]..[to] inklusif (siklus anggaran, 60 hari catat cepat). */
    @Transaction
    @Query(
        """
        SELECT * FROM transactions WHERE wallet_id = :walletId AND date BETWEEN :from AND :to
        ORDER BY date DESC, created_at DESC, id DESC
        """
    )
    fun observeBetween(walletId: Long, from: LocalDate, to: LocalDate): Flow<List<TransactionWithCategory>>

    /**
     * Semua Transaksi: cari di catatan/nama kategori (tanpa beda besar-kecil huruf untuk huruf Latin) dan filter nama
     * kategori ([category] null = semua), terbaru dulu, paling banyak [limit] baris (dimuat bertahap saat discroll).
     */
    @Transaction
    @Query(
        """
        SELECT t.* FROM transactions t LEFT JOIN categories c ON c.id = t.category_id
        WHERE t.wallet_id = :walletId
          AND (:category IS NULL OR c.name = :category)
          AND (:query = '' OR t.note LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%')
        ORDER BY t.date DESC, t.created_at DESC, t.id DESC
        LIMIT :limit
        """
    )
    fun observeSearch(walletId: Long, query: String, category: String?, limit: Int): Flow<List<TransactionWithCategory>>

    /** Pemasukan & pengeluaran (bukan transfer) [from]..[to]. */
    @Query(
        """
        SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount END), 0) AS income,
               COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount END), 0) AS expense,
               COUNT(*) AS count
        FROM transactions WHERE wallet_id = :walletId AND date BETWEEN :from AND :to
        """
    )
    fun observeTotals(walletId: Long, from: LocalDate, to: LocalDate): Flow<TotalsRow>

    /** Total per kategori untuk satu jenis, terbesar dulu (Insight). */
    @Query(
        """
        SELECT c.*, COUNT(t.id) AS tx_count, SUM(t.amount) AS total
        FROM transactions t JOIN categories c ON c.id = t.category_id
        WHERE t.wallet_id = :walletId AND t.type = :type AND t.date BETWEEN :from AND :to
        GROUP BY c.id ORDER BY total DESC
        """
    )
    fun observeCategoryTotals(walletId: Long, type: TransactionType, from: LocalDate, to: LocalDate): Flow<List<CategoryTotalRow>>

    @Query("SELECT MIN(date) FROM transactions WHERE wallet_id = :walletId")
    fun observeFirstDate(walletId: Long): Flow<LocalDate?>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE wallet_id = :walletId)")
    fun observeHasAny(walletId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE wallet_id = :walletId AND date = :date)")
    suspend fun hasOn(walletId: Long, date: LocalDate): Boolean

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

data class TotalsRow(val income: Long, val expense: Long, val count: Int)

data class CategoryTotalRow(
    @Embedded val category: CategoryEntity,
    @ColumnInfo(name = "tx_count") val count: Int,
    val total: Long,
)
