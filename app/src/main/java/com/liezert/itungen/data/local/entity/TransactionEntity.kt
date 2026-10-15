package com.liezert.itungen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.liezert.itungen.domain.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["wallet_id"]),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"]),
    ],
    indices = [
        Index("date"),
        Index("wallet_id", "date"),
        Index("category_id"),
        Index("uuid", unique = true),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    /** Kosong untuk transfer. */
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    val type: TransactionType,
    /** Rupiah bulat, selalu > 0. */
    val amount: Long,
    val note: String? = null,
    /** Tanggal lokal transaksi, terpisah dari [createdAt]. Disimpan sebagai epoch-day. */
    val date: LocalDate,
    /** Pengikat dua baris transfer (TRANSFER_OUT + TRANSFER_IN). */
    @ColumnInfo(name = "transfer_group_id") val transferGroupId: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    /** Identitas lintas perangkat untuk sinkron (id lokal bisa bentrok antar-HP). */
    @ColumnInfo(defaultValue = "") val uuid: String = UUID.randomUUID().toString(),
    /** false = perubahan lokal belum terunggah ke akun. */
    @ColumnInfo(defaultValue = "0") val synced: Boolean = false,
    /**
     * Waktu terakhir diubah (epoch ms, jam HP); 0 = belum pernah diubah sejak dicatat. Penentu bentrok sinkron:
     * perubahan terakhir menang.
     */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0,
)

data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @Relation(parentColumn = "category_id", entityColumn = "id") val category: CategoryEntity?,
)
