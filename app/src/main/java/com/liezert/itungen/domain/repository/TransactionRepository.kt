package com.liezert.itungen.domain.repository

import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.RecurringRule
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionRepository {
    /** Terbaru dulu. */
    fun observeTransactions(): Flow<List<Transaction>>

    /** Total Saldo = saldo dompet utama (§3.2). */
    fun observeMainBalance(): Flow<Long>

    fun observeCategories(type: CategoryType): Flow<List<Category>>

    /**
     * Mencatat pemasukan/pengeluaran di dompet utama. Bila [repeatMonthly], dibuat aturan bulanan
     * mulai [date] (gaji / tagihan tetap) dan transaksinya dicatat oleh [generateRecurring].
     */
    suspend fun addToMainWallet(
        type: TransactionType,
        amount: Long,
        categoryId: Long,
        note: String?,
        date: LocalDate,
        repeatMonthly: Boolean = false,
    )

    /** Mencatat semua jatuh tempo aturan bulanan sampai hari ini; aman dijalankan berulang. Mengembalikan jumlah baris baru. */
    suspend fun generateRecurring(): Int

    /** Aktif dulu. */
    fun observeRecurringRules(): Flow<List<RecurringRule>>

    /** Diaktifkan kembali: bulan-bulan selama nonaktif tidak dikejar, pencatatan mulai lagi hari ini. */
    suspend fun setRecurringRuleActive(id: Long, active: Boolean)

    /** Transaksi yang sudah tercatat dari aturan ini tetap ada. */
    suspend fun deleteRecurringRule(id: Long)
}
