package com.liezert.itungen.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Migrasi tidak boleh menghapus data keuangan (§7.4). */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    // Helper menulis ke folder database app yang terpasang; jangan tinggalkan sampah di HP.
    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(dbName)
        File(context.getDatabasePath(dbName).path + ".lck").delete() // file kunci room-testing
    }

    @Test
    fun migrate1To2_keepsTransactions() {
        helper.createDatabase(dbName, 1).apply {
            execSQL("INSERT INTO wallets (id, name, type, is_archived, created_at) VALUES (1, 'Saldo ItungEn', 'MAIN', 0, 0)")
            execSQL("INSERT INTO categories (id, name, icon, color, type, is_default) VALUES (1, 'Makanan', 'food', 0, 'EXPENSE', 1)")
            execSQL(
                "INSERT INTO transactions (wallet_id, category_id, type, amount, date, created_at) " +
                    "VALUES (1, 1, 'EXPENSE', 18000, 20720, 0)"
            )
            close()
        }

        // Memvalidasi skema hasil migrasi terhadap app/schemas/.../2.json.
        val db = helper.runMigrationsAndValidate(dbName, 2, true)

        db.query("SELECT amount FROM transactions").use {
            it.moveToFirst()
            assertEquals(1, it.count)
            assertEquals(18_000L, it.getLong(0))
        }
        db.query("SELECT COUNT(*) FROM recurring_rules").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate2To3_keepsDataAndAddsGoals() {
        helper.createDatabase(dbName, 2).apply {
            execSQL("INSERT INTO wallets (id, name, type, is_archived, created_at) VALUES (1, 'Saldo ItungEn', 'MAIN', 0, 0)")
            execSQL("INSERT INTO transactions (wallet_id, type, amount, date, created_at) VALUES (1, 'INCOME', 1000000, 20720, 0)")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 3, true)
        db.query("SELECT amount FROM transactions").use {
            it.moveToFirst()
            assertEquals(1_000_000L, it.getLong(0))
        }
        db.query("SELECT COUNT(*) FROM goals").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate3To4_dropsRecurringButKeepsItsTransactions() {
        helper.createDatabase(dbName, 3).apply {
            execSQL("INSERT INTO wallets (id, name, type, is_archived, created_at) VALUES (1, 'Saldo ItungEn', 'MAIN', 0, 0)")
            execSQL("INSERT INTO categories (id, name, icon, color, type, is_default) VALUES (1, 'Gaji', 'salary', 0, 'INCOME', 1)")
            execSQL(
                "INSERT INTO recurring_rules (id, wallet_id, category_id, type, amount, day_of_month, start_date, is_active) " +
                    "VALUES (1, 1, 1, 'INCOME', 3000000, 25, 20720, 1)"
            )
            // Satu transaksi hasil aturan berulang + satu manual.
            execSQL(
                "INSERT INTO transactions (wallet_id, category_id, type, amount, note, date, recurring_rule_id, created_at) " +
                    "VALUES (1, 1, 'INCOME', 3000000, 'Gaji', 20720, 1, 0)"
            )
            execSQL("INSERT INTO transactions (wallet_id, type, amount, date, created_at) VALUES (1, 'EXPENSE', 18000, 20721, 0)")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 4, true)
        db.query("SELECT amount, note FROM transactions ORDER BY date").use {
            assertEquals(2, it.count)
            it.moveToFirst()
            assertEquals(3_000_000L, it.getLong(0))
            assertEquals("Gaji", it.getString(1))
        }
        db.query("SELECT COUNT(*) FROM sqlite_master WHERE name = 'recurring_rules'").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate4To5_givesEveryRowAUuid_andMarksDataForUpload() {
        helper.createDatabase(dbName, 4).apply {
            execSQL("INSERT INTO wallets (id, name, type, is_archived, created_at) VALUES (1, 'Saldo ItungEn', 'MAIN', 0, 0)")
            execSQL("INSERT INTO wallets (id, name, type, is_archived, created_at) VALUES (2, 'Sepatu', 'GOAL', 0, 0)")
            execSQL("INSERT INTO categories (id, name, icon, color, type, is_default) VALUES (1, 'Makanan', 'food', 0, 'EXPENSE', 1)")
            execSQL(
                "INSERT INTO goals (id, wallet_id, title, target_amount, planned_monthly, status, created_at) " +
                    "VALUES (1, 2, 'Sepatu', 800000, 0, 'ACTIVE', 0)"
            )
            execSQL("INSERT INTO transactions (wallet_id, category_id, type, amount, date, created_at) VALUES (1, 1, 'EXPENSE', 18000, 20720, 0)")
            execSQL("INSERT INTO transactions (wallet_id, category_id, type, amount, date, created_at) VALUES (1, 1, 'EXPENSE', 5000, 20721, 0)")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 5, true, Migration4To5)

        db.query("SELECT uuid FROM wallets ORDER BY id").use {
            it.moveToFirst()
            assertEquals(MAIN_WALLET_UUID, it.getString(0))
            it.moveToNext()
            assertEquals(32, it.getString(0).length) // dompet goal: uuid acak
        }
        db.query("SELECT uuid FROM categories").use {
            it.moveToFirst()
            assertEquals("default-expense-food", it.getString(0))
        }
        // Dua transaksi lama: uuid berbeda, belum tersinkron (akan terunggah saat pertama masuk).
        db.query("SELECT COUNT(DISTINCT uuid), SUM(synced), SUM(amount) FROM transactions").use {
            it.moveToFirst()
            assertEquals(2, it.getInt(0))
            assertEquals(0, it.getInt(1))
            assertEquals(23_000L, it.getLong(2))
        }
        db.query("SELECT synced, length(uuid) FROM goals").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
            assertEquals(32, it.getInt(1))
        }
    }

    @Test
    fun migrate5To6_keepsTransactions_andAddsDeleteQueue() {
        helper.createDatabase(dbName, 5).apply {
            execSQL("INSERT INTO wallets (id, name, type, is_archived, created_at, uuid) VALUES (1, 'Saldo ItungEn', 'MAIN', 0, 0, 'main')")
            execSQL("INSERT INTO transactions (wallet_id, type, amount, date, created_at, uuid, synced) VALUES (1, 'EXPENSE', 18000, 20720, 0, 'a', 1)")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 6, true)
        db.query("SELECT amount FROM transactions").use {
            it.moveToFirst()
            assertEquals(18_000L, it.getLong(0))
        }
        db.query("SELECT COUNT(*) FROM deleted_transactions").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate6To7_keepsQueuedDeletesUnpushed() {
        helper.createDatabase(dbName, 6).apply {
            execSQL("INSERT INTO deleted_transactions (uuid) VALUES ('a')")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 7, true)
        db.query("SELECT pushed FROM deleted_transactions WHERE uuid = 'a'").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0)) // masih harus dikirim ke server
        }
    }
}

