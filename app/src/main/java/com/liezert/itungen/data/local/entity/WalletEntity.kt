package com.liezert.itungen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.liezert.itungen.domain.model.WalletType
import java.time.Instant
import java.util.UUID

/** Dompet tidak pernah dihapus, hanya diarsipkan. Saldo dihitung dari transaksi, tidak disimpan. */
@Entity(tableName = "wallets", indices = [Index("uuid", unique = true)])
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: WalletType,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    /** Identitas lintas perangkat untuk sinkron (id lokal bisa bentrok antar-HP). */
    @ColumnInfo(defaultValue = "") val uuid: String = UUID.randomUUID().toString(),
)
