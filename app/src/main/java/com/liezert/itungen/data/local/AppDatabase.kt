package com.liezert.itungen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.liezert.itungen.data.local.dao.CategoryDao
import com.liezert.itungen.data.local.dao.TransactionDao
import com.liezert.itungen.data.local.dao.WalletDao
import com.liezert.itungen.data.local.entity.CategoryEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.local.entity.WalletEntity
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.CategoryType.EXPENSE
import com.liezert.itungen.domain.model.CategoryType.INCOME
import java.time.Instant
import java.time.LocalDate

// Jangan pernah fallbackToDestructiveMigration: setiap versi baru wajib Migration eksplisit (§7.4).
@Database(
    entities = [WalletEntity::class, CategoryEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        /** [name] null = database in-memory, untuk test. */
        fun create(context: Context, name: String? = "itungen.db"): AppDatabase =
            (if (name == null) Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            else Room.databaseBuilder(context, AppDatabase::class.java, name))
                .addCallback(SeedCallback)
                .build()
    }
}

class Converters {
    @TypeConverter fun localDateToEpochDay(date: LocalDate): Long = date.toEpochDay()
    @TypeConverter fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
    @TypeConverter fun instantToMillis(instant: Instant): Long = instant.toEpochMilli()
    @TypeConverter fun millisToInstant(millis: Long): Instant = Instant.ofEpochMilli(millis)
}

// ponytail: ikon dan warna sementara; samakan dengan Figma saat layar kategori digambar.
private val DefaultCategories: List<Triple<String, String, CategoryType>> = listOf(
    Triple("Makanan", "food", EXPENSE),
    Triple("Transportasi", "transport", EXPENSE),
    Triple("Belanja", "shopping", EXPENSE),
    Triple("Tagihan", "bill", EXPENSE),
    Triple("Hiburan", "entertainment", EXPENSE),
    Triple("Kesehatan", "health", EXPENSE),
    Triple("Pendidikan", "education", EXPENSE),
    Triple("Lainnya", "other", EXPENSE),
    Triple("Gaji", "salary", INCOME),
    Triple("Kiriman", "gift", INCOME),
    Triple("Bonus", "bonus", INCOME),
    Triple("Lainnya", "other", INCOME),
)
private const val ExpenseColor = 0xFFFF453AL
private const val IncomeColor = 0xFF32D74BL

/** Isi awal saat database pertama kali dibuat; berjalan dalam transaksi pembuatan skema. */
private object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO wallets (name, type, is_archived, created_at) VALUES (?, 'MAIN', 0, ?)",
            arrayOf<Any>("Saldo ItungEn", System.currentTimeMillis()),
        )
        for ((name, icon, type) in DefaultCategories) {
            db.execSQL(
                "INSERT INTO categories (name, icon, color, type, is_default) VALUES (?, ?, ?, ?, 1)",
                arrayOf<Any>(name, icon, if (type == EXPENSE) ExpenseColor else IncomeColor, type.name),
            )
        }
    }
}
