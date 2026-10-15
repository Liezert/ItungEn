package com.liezert.itungen.data.sync

import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.local.dao.PendingTransaction
import com.liezert.itungen.data.repository.RoomTransactionRepository
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

/** Menerapkan dokumen dari server tanpa Firestore: hanya Room + [applyRemoteTransaction]. */
@RunWith(AndroidJUnit4::class)
class ApplyRemoteTest {
    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 10, 15)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private lateinit var money: RoomTransactionRepository

    @Before
    fun setUp() {
        db = AppDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        money = RoomTransactionRepository(db, clock)
    }

    @After
    fun tearDown() = db.close()

    /**
     * Bug: transaksi baru (belum tersinkron) sedang diunggah, lalu dihapus sebelum unggahan selesai. Unggahan tetap
     * menulis dokumennya ke server; tarikan sesudahnya membawa dokumen itu kembali dan transaksi muncul lagi,
     * sedangkan penanda hapusnya tidak pernah terkirim (HP lain tetap menyimpannya).
     */
    @Test
    fun deleteDuringUpload_staysDeleted_andDeleteIsSent() = runBlocking<Unit> {
        val food = money.observeCategories(CategoryType.EXPENSE).first().first { it.name == "Makanan" }
        val id = money.addToMainWallet(TransactionType.EXPENSE, 12_000, food.id, "Makan", today)
        val dao = db.syncDao()
        val uploading = dao.pendingTransactions().single() // dibaca unggahan, belum di-commit

        money.deleteTransaction(id)
        // Unggahan selesai: dokumen yang sudah terbaca tetap terkirim, lalu tarikan membawanya kembali.
        db.withTransaction { applyRemoteTransaction(dao, uploading.tx.uuid, uploading.toDoc()) }

        assertEquals(emptyList<Any>(), money.allTransactions())
        assertEquals(listOf(uploading.tx.uuid), dao.pendingDeletes()) // penanda hapus tetap akan dikirim
    }

    /** Urungkan hapus di HP lain: dokumen yang dikirim ulang memulihkan transaksi yang sudah dihapus di sini. */
    @Test
    fun undoOnOtherPhone_restoresRow() = runBlocking<Unit> {
        val food = money.observeCategories(CategoryType.EXPENSE).first().first { it.name == "Makanan" }
        money.addToMainWallet(TransactionType.EXPENSE, 12_000, food.id, "Makan", today)
        val dao = db.syncDao()
        val row = dao.pendingTransactions().single()
        dao.markTransactionsSynced(listOf(row.tx.uuid))
        db.withTransaction { applyRemoteTransaction(dao, row.tx.uuid, mapOf("deleted" to true)) } // dihapus di HP lain
        assertEquals(emptyList<Any>(), money.allTransactions())

        db.withTransaction { applyRemoteTransaction(dao, row.tx.uuid, row.toDoc()) } // lalu diurungkan di sana
        assertEquals(listOf(12_000L), money.allTransactions().map { it.amount }) // id lokal boleh baru
        assertEquals(emptyList<String>(), dao.deletedUuids())
    }

    /** Ubah di HP lain diterapkan ke baris lokal yang sudah tersinkron (sebelumnya diabaikan: transaksi dianggap tetap). */
    @Test
    fun remoteEdit_updatesSyncedRow() = runBlocking<Unit> {
        val row = syncedRow()
        db.withTransaction { applyRemoteTransaction(db.syncDao(), row.tx.uuid, row.toDoc() + mapOf("amount" to 30_000L, "updatedAt" to 5_000L)) }
        assertEquals(listOf(30_000L), money.allTransactions().map { it.amount })
    }

    /** Bentrok: diubah di dua HP sebelum sinkron → yang terakhir diubah menang. */
    @Test
    fun editConflict_lastWriteWins() = runBlocking<Unit> {
        val row = syncedRow()
        val id = money.allTransactions().single().id
        money.updateTransaction(id, TransactionType.EXPENSE, 20_000, row.tx.categoryId!!, "Lokal", today)
        val localAt = db.transactionDao().get(id).updatedAt

        // Server lebih lama: perubahan lokal tetap, menunggu diunggah.
        db.withTransaction { applyRemoteTransaction(db.syncDao(), row.tx.uuid, row.toDoc() + mapOf("note" to "Lama", "updatedAt" to localAt - 1)) }
        assertEquals("Lokal", money.allTransactions().single().note)
        // Server lebih baru: menang.
        db.withTransaction { applyRemoteTransaction(db.syncDao(), row.tx.uuid, row.toDoc() + mapOf("note" to "Baru", "updatedAt" to localAt + 1)) }
        assertEquals("Baru", money.allTransactions().single().note)
        assertEquals(true, db.transactionDao().get(id).synced)
    }

    /** Hapus menang atas ubah: dihapus di sini, diubah di HP lain → tetap terhapus, penanda tetap dikirim. */
    @Test
    fun deleteBeatsRemoteEdit() = runBlocking<Unit> {
        val row = syncedRow()
        money.deleteTransaction(money.allTransactions().single().id)
        db.withTransaction { applyRemoteTransaction(db.syncDao(), row.tx.uuid, row.toDoc() + mapOf("updatedAt" to 4_000_000_000_000L)) }
        assertEquals(emptyList<Any>(), money.allTransactions())
        assertEquals(listOf(row.tx.uuid), db.syncDao().pendingDeletes())
    }

    /** Diubah lagi selama unggahan berjalan: tetap ditandai belum tersinkron. */
    @Test
    fun editDuringUpload_staysPending() = runBlocking<Unit> {
        val food = money.observeCategories(CategoryType.EXPENSE).first().first { it.name == "Makanan" }
        val id = money.addToMainWallet(TransactionType.EXPENSE, 12_000, food.id, "Makan", today)
        val uploading = db.syncDao().pendingTransactions().single()
        money.updateTransaction(id, TransactionType.EXPENSE, 15_000, food.id, "Makan", today)
        db.syncDao().markTransactionSynced(uploading.tx.uuid, uploading.tx.updatedAt)
        assertEquals(false, db.transactionDao().get(id).synced)
    }

    private suspend fun syncedRow(): PendingTransaction {
        val food = money.observeCategories(CategoryType.EXPENSE).first().first { it.name == "Makanan" }
        money.addToMainWallet(TransactionType.EXPENSE, 12_000, food.id, "Makan", today)
        val row = db.syncDao().pendingTransactions().single()
        db.syncDao().markTransactionsSynced(listOf(row.tx.uuid))
        return row
    }
}
