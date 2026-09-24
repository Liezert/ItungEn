package com.liezert.itungen.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.liezert.itungen.domain.model.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Kunci ikon, dipetakan ke aset di lapisan UI. */
    val icon: String,
    /** ARGB, mis. 0xFFFF9F0A. */
    val color: Long,
    val type: CategoryType,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
)
