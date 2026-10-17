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
     * Hapus goal ("Hapus target" di detail goal). Transfer goal mengikuti goal-nya (§3.2), jadi semua setoran beserta
     * pasangannya di dompet utama ikut dihapus: saldo utama kembali seperti sebelum target dibuat, tanpa uang yang
     * hilang. Goal dan dompetnya tetap ada sebagai CANCELLED + diarsipkan (tidak tampil) agar penghapusan ikut
     * tersinkron; baris transfer yang dihapus diberi penanda hapus (akun & cadangan). File gambarnya dihapus pemanggil.
     */
    suspend fun deleteGoal(goalId: Long)
}
