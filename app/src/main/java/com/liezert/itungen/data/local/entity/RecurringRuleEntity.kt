package com.liezert.itungen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.liezert.itungen.domain.model.TransactionType
import java.time.LocalDate

/** Aturan bulanan: gaji = rule INCOME, tagihan tetap = rule EXPENSE (§3.1). */
@Entity(
    tableName = "recurring_rules",
    foreignKeys = [
        ForeignKey(entity = WalletEntity::class, parentColumns = ["id"], childColumns = ["wallet_id"]),
        ForeignKey(entity = CategoryEntity::class, parentColumns = ["id"], childColumns = ["category_id"]),
    ],
    indices = [Index("wallet_id"), Index("category_id")],
)
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val type: TransactionType,
    val amount: Long,
    val note: String? = null,
    @ColumnInfo(name = "day_of_month") val dayOfMonth: Int,
    @ColumnInfo(name = "start_date") val startDate: LocalDate,
    @ColumnInfo(name = "end_date") val endDate: LocalDate? = null,
    @ColumnInfo(name = "last_generated_date") val lastGeneratedDate: LocalDate? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
)
