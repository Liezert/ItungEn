package com.liezert.itungen.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

/** Hapus transaksi: saldo ikut berubah, dan setiap penghapusan tercatat untuk disinkronkan. */
@RunWith(AndroidJUnit4::class)
class DeleteTest {
    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 10, 14)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private lateinit var money: RoomTransactionRepository

    @Before
    fun setUp() {
        db = AppDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        money = RoomTransactionRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun delete_updatesBalance_andQueuesTombstones() = runBlocking<Unit> {
        val food = money.observeCategories(CategoryType.EXPENSE).first().first { it.name == "Makanan" }
        money.addToMainWallet(TransactionType.EXPENSE, 10_000, food.id, "Belum terunggah", today)
        money.addToMainWallet(TransactionType.EXPENSE, 20_000, food.id, "Sudah terunggah", today)
        val (local, synced) = money.allTransactions().sortedBy { it.amount }
        db.syncDao().markTransactionsSynced(listOf(db.transactionDao().get(synced.id).uuid))
        assertEquals(-30_000L, money.observeMainBalance().first())

        money.deleteTransaction(local.id)
        assertEquals(1, db.syncDao().pendingDeletes().size) // unggahannya bisa sedang berjalan: penanda tetap dikirim
        money.deleteTransaction(synced.id)
        assertEquals(2, db.syncDao().pendingDeletes().size) // HP lain perlu ikut menghapus

        assertEquals(0L, money.observeMainBalance().first())
        assertEquals(emptyList<Any>(), money.allTransactions())
        assertEquals(2, db.syncDao().deletedUuids().size) // keduanya tetap tercatat untuk pemulihan cadangan
    }

    @Test
    fun undo_restoresSameRow_andClearsTombstone() = runBlocking<Unit> {
        val food = money.observeCategories(CategoryType.EXPENSE).first().first { it.name == "Makanan" }
        val id = money.addToMainWallet(TransactionType.EXPENSE, 15_000, food.id, "Makan", today)
        val uuid = db.transactionDao().get(id).uuid
        db.syncDao().markTransactionsSynced(listOf(uuid))

        val undo = money.deleteTransaction(id)
        undo()

        val restored = db.transactionDao().get(id)
        assertEquals(uuid, restored.uuid)
        assertEquals(false, restored.synced) // diunggah lagi, menimpa penanda hapus di server
        assertEquals(emptyList<String>(), db.syncDao().deletedUuids())
        assertEquals(-15_000L, money.observeMainBalance().first())
    }
}
