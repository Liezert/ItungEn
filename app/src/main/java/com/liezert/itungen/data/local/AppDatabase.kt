package com.liezert.itungen.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.liezert.itungen.data.local.dao.CategoryDao
import com.liezert.itungen.data.local.dao.GoalDao
import com.liezert.itungen.data.local.dao.SyncDao
import com.liezert.itungen.data.local.dao.TransactionDao
import com.liezert.itungen.data.local.dao.WalletDao
import com.liezert.itungen.data.local.entity.CategoryEntity
import com.liezert.itungen.data.local.entity.GoalEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.local.entity.WalletEntity
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.CategoryType.EXPENSE
import com.liezert.itungen.domain.model.CategoryType.INCOME
import java.time.Instant
import java.time.LocalDate

// Jangan pernah fallbackToDestructiveMigration: setiap versi baru wajib migrasi eksplisit yang diuji (§7.4).
@Database(
    entities = [WalletEntity::class, CategoryEntity::class, TransactionEntity::class, GoalEntity::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2), // + tabel recurring_rules (Tahap 4)
        AutoMigration(from = 2, to = 3), // + tabel goals (Tahap 4)
        AutoMigration(from = 3, to = 4, spec = DropRecurring::class), // − transaksi berulang
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun goalDao(): GoalDao
    abstract fun syncDao(): SyncDao

    companion object {
        /** [name] null = database in-memory, untuk test. */
        fun create(context: Context, name: String? = "itungen.db"): AppDatabase =
            (if (name == null) Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            else Room.databaseBuilder(context, AppDatabase::class.java, name))
                .addMigrations(Migration4To5)
                .addCallback(SeedCallback)
                .build()
    }
}

/**
 * Fitur transaksi berulang dihapus (otomatisasi gaji sering tidak cocok dengan tanggal gajian sebenarnya).
 * Aturannya ikut dihapus; transaksi yang sudah tercatat darinya tetap ada sebagai transaksi biasa.
 */
@DeleteTable(tableName = "recurring_rules")
@DeleteColumn(tableName = "transactions", columnName = "recurring_rule_id")
class DropRecurring : AutoMigrationSpec

/** uuid tetap dompet utama: sama di semua HP, jadi transaksi dompet utama dari HP lain langsung cocok. */
const val MAIN_WALLET_UUID = "main"

/** uuid tetap kategori bawaan, mis. "default-expense-food". */
fun defaultCategoryUuid(icon: String, type: CategoryType) = "default-${type.name.lowercase()}-$icon"

/**
 * + uuid (identitas lintas HP) di wallets, categories, transactions, goals; + synced di transactions dan goals.
 * Baris lama mendapat uuid acak (dompet utama & kategori bawaan: uuid tetap) dan synced = 0,
 * sehingga ikut terunggah saat pengguna pertama kali masuk dengan Google.
 */
val Migration4To5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val random = "lower(hex(randomblob(16)))"
        for (table in listOf("wallets", "categories", "transactions", "goals")) {
            db.execSQL("ALTER TABLE $table ADD COLUMN uuid TEXT NOT NULL DEFAULT ''")
        }
        for (table in listOf("transactions", "goals")) {
            db.execSQL("ALTER TABLE $table ADD COLUMN synced INTEGER NOT NULL DEFAULT 0")
            db.execSQL("UPDATE $table SET uuid = $random")
        }
        db.execSQL("UPDATE wallets SET uuid = CASE WHEN type = 'MAIN' THEN '$MAIN_WALLET_UUID' ELSE $random END")
        db.execSQL(
            "UPDATE categories SET uuid = CASE WHEN is_default = 1 " +
                "THEN 'default-' || lower(type) || '-' || icon ELSE $random END"
        )
        for (table in listOf("wallets", "categories", "transactions", "goals")) {
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_${table}_uuid ON $table (uuid)")
        }
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
            "INSERT INTO wallets (name, type, is_archived, created_at, uuid) VALUES (?, 'MAIN', 0, ?, ?)",
            arrayOf<Any>("Saldo ItungEn", System.currentTimeMillis(), MAIN_WALLET_UUID),
        )
        for ((name, icon, type) in DefaultCategories) {
            db.execSQL(
                "INSERT INTO categories (name, icon, color, type, is_default, uuid) VALUES (?, ?, ?, ?, 1, ?)",
                arrayOf<Any>(name, icon, if (type == EXPENSE) ExpenseColor else IncomeColor, type.name, defaultCategoryUuid(icon, type)),
            )
        }
    }
}
