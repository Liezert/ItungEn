package com.liezert.itungen.domain.model

import java.time.Instant
import java.time.LocalDate

data class Category(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val type: CategoryType,
)

data class Transaction(
    val id: Long,
    val walletId: Long,
    /** Null untuk transfer. */
    val category: Category?,
    val type: TransactionType,
    val amount: Long,
    val note: String?,
    val date: LocalDate,
    /** Saat dicatat; hanya untuk menampilkan jam, urutan memakai [date]. */
    val createdAt: Instant = Instant.EPOCH,
)

enum class TransactionError { AMOUNT_REQUIRED, AMOUNT_TOO_LARGE, CATEGORY_REQUIRED, DATE_IN_FUTURE }

/** Aturan F1 / §7.1. [today] diinjeksi agar bisa diuji tanpa jam sistem. */
fun validateTransaction(amount: Long?, categoryId: Long?, date: LocalDate, today: LocalDate): TransactionError? =
    when {
        amount == null || amount <= 0 -> TransactionError.AMOUNT_REQUIRED
        amount > MAX_AMOUNT -> TransactionError.AMOUNT_TOO_LARGE
        categoryId == null -> TransactionError.CATEGORY_REQUIRED
        date.isAfter(today) -> TransactionError.DATE_IN_FUTURE
        else -> null
    }
