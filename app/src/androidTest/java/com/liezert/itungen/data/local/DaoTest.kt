package com.liezert.itungen.data.local

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.local.entity.GoalEntity
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.local.entity.WalletEntity
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.GoalStatus
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.TransactionType.EXPENSE
import com.liezert.itungen.domain.model.TransactionType.INCOME
import com.liezert.itungen.domain.model.TransactionType.TRANSFER_IN
import com.liezert.itungen.domain.model.TransactionType.TRANSFER_OUT
import com.liezert.itungen.domain.model.WalletType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class DaoTest {
    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 9, 24)

    @Before
    fun setUp() {
        db = AppDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
    }

    @After
    fun tearDown() = db.close()

    private fun tx(
        walletId: Long,
        type: TransactionType,
        amount: Long,
        date: LocalDate = today,
    ) = TransactionEntity(
        walletId = walletId,
        type = type,
        amount = amount,
        date = date,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun seed_createsMainWalletAndDefaultCategories() = runBlocking<Unit> {
        assertEquals(WalletType.MAIN, db.walletDao().getMain().type)
        assertTrue(db.categoryDao().observeByType(CategoryType.EXPENSE).first().any { it.name == "Lainnya" })
        assertTrue(db.categoryDao().observeByType(CategoryType.INCOME).first().any { it.name == "Kiriman" })
    }

    @Test
    fun balance_isInflowMinusOutflow_perWallet() = runBlocking<Unit> {
        val main = db.walletDao().getMain().id
        val goal = db.walletDao().insert(WalletEntity(name = "Sepatu Lari", type = WalletType.GOAL, createdAt = Instant.EPOCH))
        val dao = db.transactionDao()
        assertEquals(0L, dao.observeBalance(main).first())

        dao.insert(tx(main, INCOME, 3_000_000))
        dao.insert(tx(main, EXPENSE, 250_000))
        dao.insert(tx(main, EXPENSE, 50_000))
        dao.insert(tx(main, TRANSFER_OUT, 500_000))
        dao.insert(tx(goal, TRANSFER_IN, 500_000))

        assertEquals(2_200_000L, dao.observeBalance(main).first())
        assertEquals(500_000L, dao.observeBalance(goal).first())
    }

    @Test
    fun observeAll_newestDateFirst() = runBlocking<Unit> {
        val main = db.walletDao().getMain().id
        db.transactionDao().insert(tx(main, EXPENSE, 1_000, date = today.minusDays(2)))
        db.transactionDao().insert(tx(main, EXPENSE, 2_000, date = today))
        db.transactionDao().insert(tx(main, EXPENSE, 3_000, date = today.minusDays(1)))

        assertEquals(
            listOf(today, today.minusDays(1), today.minusDays(2)),
            db.transactionDao().observeAll().first().map { it.transaction.date },
        )
    }

    @Test
    fun seed_usesStableUuidsAcrossDevices() = runBlocking<Unit> {
        assertEquals(MAIN_WALLET_UUID, db.walletDao().getMain().uuid)
        assertTrue(db.syncDao().categoryId(defaultCategoryUuid("food", CategoryType.EXPENSE)) != null)
        assertTrue(db.syncDao().categoryId(defaultCategoryUuid("other", CategoryType.INCOME)) != null)
    }

    @Test
    fun sync_pendingRowsCarryUuids_andGoalStatusGuardsMarkSynced() = runBlocking<Unit> {
        val main = db.walletDao().getMain().id
        val sync = db.syncDao()
        db.transactionDao().insert(tx(main, EXPENSE, 5_000))
        val goalWallet = db.walletDao().insert(WalletEntity(name = "Sepatu", type = WalletType.GOAL, createdAt = Instant.EPOCH))
        val goalId = db.goalDao().insert(
            GoalEntity(walletId = goalWallet, title = "Sepatu", targetAmount = 100, createdAt = Instant.EPOCH)
        )
        assertEquals(2, sync.pendingCount())

        val pending = sync.pendingTransactions().single()
        assertEquals(MAIN_WALLET_UUID, pending.walletUuid)
        sync.markTransactionsSynced(listOf(pending.tx.uuid))

        // Goal berubah status selama unggah: penandaan dengan status lama tidak boleh menghapus perubahan baru.
        val goal = sync.pendingGoals().single().goal
        db.goalDao().setStatus(goalId, GoalStatus.ACHIEVED)
        sync.markGoalSynced(goal.uuid, goal.status)
        assertEquals(1, sync.pendingCount())
        sync.markGoalSynced(goal.uuid, GoalStatus.ACHIEVED)
        assertEquals(0, sync.pendingCount())

        // Baris tarikan dengan uuid yang sudah ada dilewati, bukan digandakan.
        assertEquals(-1L, sync.insertTransaction(pending.tx.copy(id = 0, synced = true)))
    }
}

