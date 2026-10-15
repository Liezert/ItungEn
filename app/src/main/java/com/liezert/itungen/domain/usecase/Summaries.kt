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

/** Total pemasukan (bukan transfer) pada [month]. */
fun monthIncome(transactions: List<Transaction>, month: YearMonth): Long =
    transactions.filter { it.type == TransactionType.INCOME && YearMonth.from(it.date) == month }.sumOf { it.amount }

/** Total pengeluaran (bukan transfer) pada [month]. */
fun monthExpense(transactions: List<Transaction>, month: YearMonth): Long =
    transactions.filter { it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month }.sumOf { it.amount }

/** Transaksi yang sering diulang, untuk "Catat cepat" sekali ketuk. */
data class QuickTemplate(
    val type: TransactionType,
    val categoryId: Long,
    val note: String?,
    val amount: Long,
    /** "Makan siang" atau nama kategori bila tanpa catatan. */
    val label: String,
)

/**
 * Pengeluaran dengan kategori + catatan yang sama ≥ 2 kali dalam 60 hari terakhir, paling sering dulu; nominalnya
 * nominal terakhir ("Makan siang" 6.000 lalu 7.000 → 7.000). Tanpa pengaturan: muncul sendiri dari kebiasaan mencatat.
 * Pemasukan tidak ikut: satu ketukan tidak sengaja bisa mencatat "Gaji" dua kali.
 */
fun quickTemplates(transactions: List<Transaction>, today: LocalDate, limit: Int = 3): List<QuickTemplate> =
    transactions
        .filter { it.type == TransactionType.EXPENSE && it.category != null && it.date >= today.minusDays(60) }
        .groupBy { it.category!!.id to it.note?.trim()?.lowercase() }
        .filterValues { it.size >= 2 }
        .entries
        .sortedByDescending { it.value.size }
        .take(limit)
        .map { (_, rows) ->
            val last = rows.maxWith(compareBy({ it.date }, { it.createdAt }))
            QuickTemplate(last.type, last.category!!.id, last.note?.trim(), last.amount, last.note?.trim() ?: last.category.name)
        }

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

/** Pengeluaran (atau pemasukan, lewat [type]) per kategori pada [month], terbesar dulu. */
fun categorySpending(
    transactions: List<Transaction>,
    month: YearMonth,
    type: TransactionType = TransactionType.EXPENSE,
): List<CategorySpend> =
    transactions
        .filter { it.type == type && it.category != null && YearMonth.from(it.date) == month }
        .groupBy { it.category!!.id }
        .map { (_, rows) -> CategorySpend(rows.first().category!!, rows.size, rows.sumOf { it.amount }) }
        .sortedByDescending { it.total }

/**
 * Persen bulat yang jumlahnya tepat 100 (metode sisa terbesar): 65,2% + 34,8% → 65% + 35%, bukan 65% + 34%.
 * Semua nol → semua 0.
 */
fun roundedPercents(values: List<Long>): List<Int> {
    val total = values.sum()
    if (total <= 0) return values.map { 0 }
    val exact = values.map { it * 100.0 / total }
    val result = exact.map { it.toInt() }.toMutableList()
    val missing = 100 - result.sum()
    exact.indices.sortedByDescending { exact[it] - result[it] }.take(missing).forEach { result[it]++ }
    return result
}

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
