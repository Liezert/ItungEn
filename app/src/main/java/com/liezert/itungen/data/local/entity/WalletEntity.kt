package com.liezert.itungen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.liezert.itungen.domain.model.WalletType
import java.time.Instant

/** Dompet tidak pernah dihapus, hanya diarsipkan. Saldo dihitung dari transaksi, tidak disimpan. */
@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: WalletType,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
