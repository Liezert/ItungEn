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

/** Gaji otomatis end-to-end di atas Room: catch-up, idempoten, dan bulan berikutnya. */
@RunWith(AndroidJUnit4::class)
class RecurringTest {
    private lateinit var db: AppDatabase

    private fun repoAt(today: LocalDate) = RoomTransactionRepository(
        db,
        Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC),
    )

    @Before
    fun setUp() {
        db = AppDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun salaryRule_catchesUp_isIdempotent_andContinuesNextMonth() = runBlocking<Unit> {
        val salary = repoAt(LocalDate.of(2026, 9, 24)).observeCategories(CategoryType.INCOME).first().first { it.name == "Gaji" }

        // Gaji tanggal 25, mulai Juli; hari ini 24 Sep → Juli & Agustus tercatat, September belum.
        val repo = repoAt(LocalDate.of(2026, 9, 24))
        repo.addToMainWallet(TransactionType.INCOME, 3_000_000, salary.id, "Gaji", LocalDate.of(2026, 7, 25), repeatMonthly = true)
        assertEquals(
            listOf(LocalDate.of(2026, 8, 25), LocalDate.of(2026, 7, 25)),
            repo.observeTransactions().first().map { it.date },
        )

        // Worker dan catch-up berjalan berulang di hari yang sama: tidak ada duplikat.
        assertEquals(0, repo.generateRecurring())
        assertEquals(0, repo.generateRecurring())

        // Lewat tanggal gajian September.
        val later = repoAt(LocalDate.of(2026, 9, 25))
        assertEquals(1, later.generateRecurring())
        assertEquals(0, later.generateRecurring())
        assertEquals(9_000_000L, later.observeMainBalance().first())
    }

    @Test
    fun deactivate_reactivateWithoutBackfill_delete_keepsHistory() = runBlocking<Unit> {
        val start = repoAt(LocalDate.of(2026, 7, 25))
        val salary = start.observeCategories(CategoryType.INCOME).first().first { it.name == "Gaji" }
        start.addToMainWallet(TransactionType.INCOME, 3_000_000, salary.id, "Gaji", LocalDate.of(2026, 7, 25), repeatMonthly = true)
        val ruleId = start.observeRecurringRules().first().single().id

        // Nonaktif sejak Juli: Agustus tidak tercatat.
        start.setRecurringRuleActive(ruleId, false)
        assertEquals(0, repoAt(LocalDate.of(2026, 8, 30)).generateRecurring())

        // Aktif lagi tepat di hari gajian September: Agustus tidak dikejar, September tercatat.
        val payday = repoAt(LocalDate.of(2026, 9, 25))
        payday.setRecurringRuleActive(ruleId, true)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 7, 25)),
            payday.observeTransactions().first().map { it.date },
        )

        assertEquals(1, repoAt(LocalDate.of(2026, 10, 25)).generateRecurring())

        // Hapus aturan: riwayat tetap, pencatatan berhenti.
        payday.deleteRecurringRule(ruleId)
        assertEquals(emptyList<Any>(), payday.observeRecurringRules().first())
        assertEquals(0, repoAt(LocalDate.of(2026, 11, 25)).generateRecurring())
        assertEquals(9_000_000L, payday.observeMainBalance().first())
    }
}
