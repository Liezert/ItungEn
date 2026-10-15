package com.liezert.itungen.domain.repository

import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionRepository {
    /** Riwayat dompet utama, terbaru dulu (transfer ke goal tampil sekali, sebagai keluar). */
    fun observeTransactions(): Flow<List<Transaction>>

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

    /**
     * Menghapus pemasukan/pengeluaran saat itu juga (saldo ikut berubah). Transfer goal tidak bisa dihapus dari sini.
     * Mengembalikan aksi Urungkan yang memulihkan baris yang sama.
     */
    suspend fun deleteTransaction(id: Long): suspend () -> Unit

    /** Tanggal pertama app dipakai = saat dompet utama dibuat (§6.6 "Bergabung"). */
    suspend fun firstUseDate(): LocalDate
}
