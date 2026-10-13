package com.liezert.itungen.domain.usecase

import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

// ponytail: agregasi di memori dari daftar transaksi yang sudah dimuat; pindah ke query SQL bila riwayat sangat besar.
// Transfer (ke/dari goal) sengaja tidak dihitung sebagai pemasukan/pengeluaran (§3.2).

/** Pemasukan − pengeluaran pada [month]. */
fun monthNet(transactions: List<Transaction>, month: YearMonth): Long =
    transactions.filter { YearMonth.from(it.date) == month }.sumOf {
        when (it.type) {
            TransactionType.INCOME -> it.amount
            TransactionType.EXPENSE -> -it.amount
            else -> 0L
        }
    }

/** Awal minggu (Senin) yang memuat [today] (§5.2). */
fun weekStart(today: LocalDate): LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Total pengeluaran dari [from] sampai [to], inklusif. */
fun expenseBetween(transactions: List<Transaction>, from: LocalDate, to: LocalDate): Long =
    transactions.filter { it.type == TransactionType.EXPENSE && it.date in from..to }.sumOf { it.amount }

data class CategorySpend(val category: Category, val count: Int, val total: Long)

/** Pengeluaran per kategori pada [month], terbesar dulu. */
fun categorySpending(transactions: List<Transaction>, month: YearMonth): List<CategorySpend> =
    transactions
        .filter { it.type == TransactionType.EXPENSE && it.category != null && YearMonth.from(it.date) == month }
        .groupBy { it.category!!.id }
        .map { (_, rows) -> CategorySpend(rows.first().category!!, rows.size, rows.sumOf { it.amount }) }
        .sortedByDescending { it.total }

/** Nama chip filter: semua kategori tanpa duplikat ("Lainnya" ada di pengeluaran dan pemasukan). */
fun filterNames(expense: List<Category>, income: List<Category>): List<String> = (expense + income).map { it.name }.distinct()

/** Pencarian di judul/kategori + filter nama kategori (null = semua). Besar-kecil huruf diabaikan. */
fun filterTransactions(transactions: List<Transaction>, query: String, categoryName: String?): List<Transaction> =
    transactions.filter {
        (categoryName == null || it.category?.name == categoryName) &&
            (query.isBlank() ||
                it.note.orEmpty().contains(query, ignoreCase = true) ||
                it.category?.name.orEmpty().contains(query, ignoreCase = true))
    }
