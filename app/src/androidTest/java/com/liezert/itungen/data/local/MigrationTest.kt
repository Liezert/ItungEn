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
}
