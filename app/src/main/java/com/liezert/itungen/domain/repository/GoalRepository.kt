package com.liezert.itungen.domain.repository

import com.liezert.itungen.domain.model.Goal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface GoalRepository {
    /** Aktif dulu, lalu yang sudah tercapai; yang dibatalkan tidak ikut. */
    fun observeGoals(): Flow<List<Goal>>

    /** Membuat goal beserta dompet GOAL-nya dalam satu transaksi. */
    suspend fun createGoal(title: String, target: Long, targetDate: LocalDate?, plannedMonthly: Long): Long

    /**
     * Tambah Dana (§4.2): transfer berpasangan dompet utama → dompet goal dalam satu transaksi.
     * Status menjadi ACHIEVED bila terkumpul ≥ target.
     * @throws com.liezert.itungen.domain.usecase.InsufficientBalanceException bila saldo utama kurang.
     */
    suspend fun addFunds(goalId: Long, amount: Long)

    /** Batalkan goal (§7.2): sisa dananya dikembalikan ke dompet utama, lalu goal dan dompetnya diarsipkan. */
    suspend fun cancelGoal(goalId: Long)
}
