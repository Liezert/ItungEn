package com.liezert.itungen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Penanda transaksi yang dihapus. Yang sudah pernah terunggah dikirim sebagai dokumen `{deleted: true}` agar HP lain
 * ikut menghapusnya ([pushed] = sudah terkirim). Penanda tetap disimpan agar memulihkan cadangan lama tidak
 * memunculkan lagi transaksi yang sudah dihapus.
 */
@Entity(tableName = "deleted_transactions")
data class DeletedTransactionEntity(
    @PrimaryKey val uuid: String,
    @ColumnInfo(defaultValue = "0") val pushed: Boolean = false,
)
