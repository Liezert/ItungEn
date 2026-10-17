package com.liezert.itungen.domain.repository

import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.GoalDeposit
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface GoalRepository {
    /** Aktif dulu, lalu yang sudah tercapai; yang dibatalkan tidak ikut. */
    fun observeGoals(): Flow<List<Goal>>

    /** Membuat goal beserta dompet GOAL-nya dalam satu transaksi. */
    suspend fun createGoal(title: String, target: Long, targetDate: LocalDate?, plannedMonthly: Long, imagePath: String? = null): Long

    /** Ganti/hapus gambar produk ([imagePath] null). File lama dihapus pemanggil setelah ini berhasil. */
    suspend fun setImage(goalId: Long, imagePath: String?)

    /** Nama file gambar yang masih dipakai goal yang tampil (untuk membersihkan file yatim). */
    suspend fun imagesInUse(): Set<String>

    /**
     * Tambah Dana (§4.2): transfer berpasangan dompet utama → dompet goal dalam satu transaksi.
     * Status menjadi ACHIEVED bila terkumpul ≥ target.
     * @throws com.liezert.itungen.domain.usecase.InsufficientBalanceException bila saldo utama kurang.
     */
    suspend fun addFunds(goalId: Long, amount: Long)

    /** Riwayat Tambah Dana [goalId], terbaru dulu. */
    fun observeDeposits(goalId: Long): Flow<List<GoalDeposit>>

    /**
     * Hapus goal (§7.2, di layar "Hapus target"): sisa dananya dikembalikan ke dompet utama, lalu goal dan dompetnya
     * diarsipkan (status CANCELLED, ikut tersinkron) dan tidak tampil lagi. Riwayat transfernya tetap tercatat.
     */
    suspend fun cancelGoal(goalId: Long)
}
