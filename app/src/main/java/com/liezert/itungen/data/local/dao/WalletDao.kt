package com.liezert.itungen.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.liezert.itungen.data.local.entity.WalletEntity

@Dao
interface WalletDao {
    @Insert
    suspend fun insert(wallet: WalletEntity): Long

    /** Dompet utama selalu ada: dibuat saat database pertama kali dibuat. */
    @Query("SELECT * FROM wallets WHERE type = 'MAIN' LIMIT 1")
    suspend fun getMain(): WalletEntity

    /** Dompet tidak pernah dihapus, hanya diarsipkan (§3.2). */
    @Query("UPDATE wallets SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}
