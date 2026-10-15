package com.liezert.itungen.domain.repository

import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.usecase.CategorySpend
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Pemasukan & pengeluaran (bukan transfer, §3.2) dan jumlah semua transaksi dalam satu rentang. */
data class Totals(val income: Long, val expense: Long, val count: Int)

/**
 * Dompet utama. Layar tidak pernah memuat seluruh riwayat: setiap kueri berjendela (tanggal atau jumlah baris) atau
 * teragregasi di SQL, jadi biaya per perubahan tetap kecil walau riwayat puluhan ribu baris (docs/PERFORMANCE.md).
 */
interface TransactionRepository {
    /** [limit] transaksi terbaru, terbaru dulu (transfer ke goal tampil sekali, sebagai keluar). */
    fun observeRecent(limit: Int): Flow<List<Transaction>>

    /** Transaksi [from]..[to] inklusif, terbaru dulu. */
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>>

    /** Semua Transaksi: cari di catatan/kategori + filter nama kategori (null = semua), maksimal [limit] baris. */
    fun observeSearch(query: String, categoryName: String?, limit: Int): Flow<List<Transaction>>

    fun observeTotals(from: LocalDate, to: LocalDate): Flow<Totals>

    /** Total [type] per kategori [from]..[to], terbesar dulu. */
    fun observeCategoryTotals(type: TransactionType, from: LocalDate, to: LocalDate): Flow<List<CategorySpend>>

    /** Pengeluaran per hari [from]..[to]; hari tanpa pengeluaran tidak ada di peta. */
    fun observeDailyExpense(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Long>>

    /** Hari yang punya transaksi apa pun [from]..[to]. */
    fun observeRecordedDays(from: LocalDate, to: LocalDate): Flow<Set<LocalDate>>

    /** Tanggal transaksi paling awal; null bila belum ada. */
    fun observeFirstDate(): Flow<LocalDate?>

    fun observeHasTransactions(): Flow<Boolean>

    suspend fun hasTransactionOn(date: LocalDate): Boolean

    /** Seluruh riwayat, terbaru dulu. Hanya untuk ekspor dan tes, bukan untuk layar. */
    suspend fun allTransactions(): List<Transaction>

    /** Total Saldo = saldo dompet utama (§3.2). */
    fun observeMainBalance(): Flow<Long>

    fun observeCategories(type: CategoryType): Flow<List<Category>>

    /** Mencatat pemasukan/pengeluaran di dompet utama. Mengembalikan id (untuk Urungkan). */
    suspend fun addToMainWallet(
        type: TransactionType,
        amount: Long,
        categoryId: Long,
        note: String?,
        date: LocalDate,
    ): Long

    /** Satu transaksi; null bila sudah tidak ada (mis. dihapus di HP lain). */
    suspend fun getTransaction(id: Long): Transaction?

    /**
     * Mengubah pemasukan/pengeluaran (jenis, nominal, kategori, catatan, tanggal); saldo ikut berubah dan perubahannya
     * tersinkron. Transfer goal tidak bisa diubah dari sini.
     */
    suspend fun updateTransaction(id: Long, type: TransactionType, amount: Long, categoryId: Long, note: String?, date: LocalDate)

    /**
     * Menghapus pemasukan/pengeluaran saat itu juga (saldo ikut berubah). Transfer goal tidak bisa dihapus dari sini.
     * Mengembalikan aksi Urungkan yang memulihkan baris yang sama.
     */
    suspend fun deleteTransaction(id: Long): suspend () -> Unit

    /** Tanggal pertama app dipakai = saat dompet utama dibuat (§6.6 "Bergabung"). */
    suspend fun firstUseDate(): LocalDate
}
