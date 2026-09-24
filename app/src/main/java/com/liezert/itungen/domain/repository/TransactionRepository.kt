package com.liezert.itungen.domain.repository

import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
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

    /** Mencatat pemasukan/pengeluaran di dompet utama. */
    suspend fun addToMainWallet(type: TransactionType, amount: Long, categoryId: Long, note: String?, date: LocalDate)
}
