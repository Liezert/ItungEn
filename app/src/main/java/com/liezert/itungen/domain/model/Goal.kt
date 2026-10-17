package com.liezert.itungen.domain.model

import java.time.LocalDate

/** Satu kali Tambah Dana ke sebuah target. */
data class GoalDeposit(val amount: Long, val date: LocalDate)

/** Target tabungan; [saved] = saldo dompet GOAL-nya. */
data class Goal(
    val name: String,
    val saved: Long,
    val target: Long,
    val id: Long = 0,
    val targetDate: LocalDate? = null,
    val plannedMonthly: Long = 0,
    val status: GoalStatus = GoalStatus.ACTIVE,
    /** Nama file gambar produk (GoalImageStore); null = belum ada gambar. */
    val imagePath: String? = null,
) {
    val progress: Float get() = if (target <= 0) 0f else (saved.toFloat() / target).coerceIn(0f, 1f)
    val percent: Int get() = (progress * 100).toInt()
    val remaining: Long get() = (target - saved).coerceAtLeast(0)
}
