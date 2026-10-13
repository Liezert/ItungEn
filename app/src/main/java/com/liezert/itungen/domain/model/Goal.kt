package com.liezert.itungen.domain.model

import java.time.LocalDate

/** Target tabungan; [saved] = saldo dompet GOAL-nya. */
data class Goal(
    val name: String,
    val saved: Long,
    val target: Long,
    val id: Long = 0,
    val targetDate: LocalDate? = null,
    val plannedMonthly: Long = 0,
    val status: GoalStatus = GoalStatus.ACTIVE,
) {
    val progress: Float get() = if (target <= 0) 0f else (saved.toFloat() / target).coerceIn(0f, 1f)
    val percent: Int get() = (progress * 100).toInt()
    val remaining: Long get() = (target - saved).coerceAtLeast(0)
}
