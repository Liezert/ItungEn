package com.liezert.itungen.domain.usecase

import com.liezert.itungen.domain.model.MAX_AMOUNT
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class GoalError { TITLE_REQUIRED, TARGET_REQUIRED, TARGET_TOO_LARGE, DATE_IN_PAST, PLAN_ABOVE_TARGET }

/** Buat Target (§7.1): nama wajib, target > 0, tanggal target (opsional) tidak di masa lalu, setoran ≤ target. */
fun validateGoal(title: String, target: Long?, targetDate: LocalDate?, today: LocalDate, plannedMonthly: Long? = null): GoalError? = when {
    title.isBlank() -> GoalError.TITLE_REQUIRED
    target == null || target <= 0 -> GoalError.TARGET_REQUIRED
    target > MAX_AMOUNT -> GoalError.TARGET_TOO_LARGE
    targetDate != null && targetDate.isBefore(today) -> GoalError.DATE_IN_PAST
    plannedMonthly != null && plannedMonthly > target -> GoalError.PLAN_ABOVE_TARGET
    else -> null
}

enum class TransferError { AMOUNT_REQUIRED, INSUFFICIENT_BALANCE }

/** Tambah Dana (§7.1): nominal > 0 dan tidak melebihi saldo dompet utama. */
fun validateTransfer(amount: Long?, mainBalance: Long): TransferError? = when {
    amount == null || amount <= 0 -> TransferError.AMOUNT_REQUIRED
    amount > mainBalance -> TransferError.INSUFFICIENT_BALANCE
    else -> null
}

/** Dilempar repository bila saldo berubah antara validasi di layar dan penulisan (ditolak, tidak ada yang tertulis). */
class InsufficientBalanceException : IllegalStateException("Saldo dompet utama tidak cukup")

/**
 * Nilai saran setoran bulanan (§5.1) = sisa target ÷ sisa bulan sampai [targetDate], dibulatkan ke atas
 * ke kelipatan Rp 1.000. Bulan yang baru berjalan sebagian dihitung satu bulan; minimal satu bulan.
 * Null bila tidak ada tanggal target.
 */
fun suggestedMonthly(target: Long, saved: Long, targetDate: LocalDate?, today: LocalDate): Long? {
    if (targetDate == null) return null
    val remaining = (target - saved).coerceAtLeast(0)
    if (remaining == 0L) return 0
    var months = ChronoUnit.MONTHS.between(today, targetDate)
    if (today.plusMonths(months).isBefore(targetDate)) months++
    months = months.coerceAtLeast(1)
    val perMonth = (remaining + months - 1) / months
    return (perMonth + 999) / 1000 * 1000
}
