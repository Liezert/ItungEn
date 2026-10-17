package com.liezert.itungen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.liezert.itungen.domain.model.GoalStatus
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Target tabungan. Setiap goal punya dompet GOAL sendiri (1:1); dana terkumpul = saldo dompet itu (§3.1). */
@Entity(
    tableName = "goals",
    foreignKeys = [ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["wallet_id"])],
    indices = [Index("wallet_id", unique = true), Index("uuid", unique = true)],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    val title: String,
    @ColumnInfo(name = "target_amount") val targetAmount: Long,
    @ColumnInfo(name = "target_date") val targetDate: LocalDate? = null,
    /** Setoran bulanan yang direncanakan; diisi manual, dengan nilai saran (§5.1). */
    @ColumnInfo(name = "planned_monthly") val plannedMonthly: Long = 0,
    val status: GoalStatus = GoalStatus.ACTIVE,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
    /** Identitas lintas perangkat untuk sinkron (id lokal bisa bentrok antar-HP). */
    @ColumnInfo(defaultValue = "") val uuid: String = UUID.randomUUID().toString(),
    /** false = perubahan lokal belum terunggah ke akun. */
    @ColumnInfo(defaultValue = "0") val synced: Boolean = false,
    /** Nama file gambar produk di `filesDir/goal_images` (GoalImageStore); hanya di HP ini, tidak tersinkron. */
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
)

data class GoalWithSaved(
    @Embedded val goal: GoalEntity,
    val saved: Long,
)
