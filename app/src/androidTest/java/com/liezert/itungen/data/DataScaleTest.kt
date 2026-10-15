package com.liezert.itungen.data

import android.util.Log
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.data.local.AppDatabase
import com.liezert.itungen.data.local.entity.TransactionEntity
import com.liezert.itungen.data.repository.RoomTransactionRepository
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.usecase.BudgetPlan
import com.liezert.itungen.domain.usecase.computeBudget
import com.liezert.itungen.domain.usecase.quickTemplates
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Pengukuran (docs/PERFORMANCE.md), bukan pemeriksaan benar/salah: berapa lama Beranda + Insight menghitung ulang
 * setelah satu perubahan bila ada 30.000 transaksi (kueri berjendela/teragregasi). Hasil di logcat tag "DataScale".
 * adb shell am instrument -w -e class com.liezert.itungen.data.DataScaleTest com.liezert.itungen.test/androidx.test.runner.AndroidJUnitRunner
 */
@RunWith(AndroidJUnit4::class)
class DataScaleTest {
    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 10, 15)
    private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
    private lateinit var money: RoomTransactionRepository

    @Before
    fun setUp() = runBlocking<Unit> {
        db = AppDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        money = RoomTransactionRepository(db, clock)
        val expense = money.observeCategories(CategoryType.EXPENSE).first().map { it.id }
        val mainId = db.walletDao().getMain().id
        val random = Random(42)
        db.withTransaction {
            repeat(Rows) { i ->
                db.transactionDao().insert(
                    TransactionEntity(
                        walletId = mainId, categoryId = expense.random(random), type = TransactionType.EXPENSE,
                        amount = random.nextLong(1, 200) * 1_000, note = "Uji ${i % 50}",
                        date = today.minusDays(random.nextLong(0, 1_100)), createdAt = Instant.EPOCH,
                    )
                )
            }
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun homeAndInsight_recompute() = runBlocking<Unit> {
        val plan = BudgetPlan(income = 3_000_000, cycleStartDay = 25)
        val month = YearMonth.from(today)
        val previous = month.minusMonths(1)
        val times = (1..5).map {
            measureTimeMillis {
                // Beranda: total bulan ini, jendela siklus + 60 hari, riwayat terakhir.
                money.observeTotals(month.atDay(1), month.atEndOfMonth()).first()
                val window = money.observeBetween(today.minusDays(60), today).first()
                money.observeRecent(4).first()
                computeBudget(plan, window, 0, today)
                quickTemplates(window, today)
                // Insight: per kategori bulan ini dan bulan lalu.
                money.observeCategoryTotals(TransactionType.EXPENSE, month.atDay(1), month.atEndOfMonth()).first()
                money.observeCategoryTotals(TransactionType.EXPENSE, previous.atDay(1), previous.atEndOfMonth()).first()
                money.observeFirstDate().first()
            }
        }
        // Rincian per langkah (putaran terakhir, cache SQLite sudah hangat).
        val steps = listOf<Pair<String, suspend () -> Unit>>(
            "totals" to { money.observeTotals(month.atDay(1), month.atEndOfMonth()).first() },
            "window60" to { money.observeBetween(today.minusDays(60), today).first() },
            "recent" to { money.observeRecent(4).first() },
            "categoryTotals" to { money.observeCategoryTotals(TransactionType.EXPENSE, month.atDay(1), month.atEndOfMonth()).first() },
            "firstDate" to { money.observeFirstDate().first() },
        ).map { (name, step) -> name to measureTimeMillis { step() } }
        Log.i("DataScale", "steps ms=$steps")
        // Pembanding: memuat seluruh riwayat (cara lama sebelum Fase C).
        val loadAll = (1..3).map { measureTimeMillis { money.allTransactions() } }
        Log.i("DataScale", "rows=$Rows windowed ms=$times median=${times.sorted()[2]} | load-all ms=$loadAll")
    }

    private companion object {
        const val Rows = 30_000
    }
}
