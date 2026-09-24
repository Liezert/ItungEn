package com.liezert.itungen.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.liezert.itungen.data.local.entity.CategoryEntity
import com.liezert.itungen.domain.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY id")
    fun observeByType(type: CategoryType): Flow<List<CategoryEntity>>
}
