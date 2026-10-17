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
    /** Isi bar progres, 0..1: setoran yang melebihi target tidak membuat bar meluber. */
    val progress: Float get() = if (target <= 0) 0f else (saved.toFloat() / target).coerceIn(0f, 1f)

    /** Persen sebenarnya, dibulatkan ke bawah (99,9% belum "100%"); bisa di atas 100 bila setoran melebihi target. */
    val percent: Int get() = if (target <= 0) 0 else (saved.coerceAtLeast(0) * 100 / target).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    /** Terkumpul sudah mencapai target (dari nominal, tidak menunggu status di database). */
    val achieved: Boolean get() = target > 0 && saved >= target
    val remaining: Long get() = (target - saved).coerceAtLeast(0)
}
