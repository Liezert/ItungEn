package com.liezert.itungen.work

import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Fase 3: nominal dari kolom ketik notifikasi (RemoteInput) dicatat sebagai pemasukan di dompet utama, lewat broadcast
 * sungguhan ke receiver (goAsync), tanpa membuka app. Input tidak valid tidak mencatat apa pun.
 */
@RunWith(AndroidJUnit4::class)
class QuickIncomeReceiverTest {
    private val app = ApplicationProvider.getApplicationContext<ItungEnApp>()
    private val money = app.container.transactionRepository

    private fun send(typed: String, categoryId: Long, label: String) {
        val intent = Intent(app, QuickIncomeReceiver::class.java)
            .setAction(QuickIncomeReceiver.ACTION_RECORD)
            .putExtra(QuickIncomeReceiver.EXTRA_CATEGORY, categoryId)
            .putExtra(QuickIncomeReceiver.EXTRA_LABEL, label)
        RemoteInput.addResultsToIntent(
            arrayOf(RemoteInput.Builder(QuickIncome.KEY_AMOUNT).build()),
            intent,
            Bundle().apply { putCharSequence(QuickIncome.KEY_AMOUNT, typed) },
        )
        app.sendBroadcast(intent)
    }

    private suspend fun incomes() = money.allTransactions().filter { it.type == TransactionType.INCOME }

    @Test
    fun typedAmount_isRecordedAsIncome() = runBlocking<Unit> {
        val salary = money.observeCategories(CategoryType.INCOME).first().first { it.name == "Gaji" }
        val before = incomes().size
        send("Rp 1,5jt", salary.id, salary.name)
        val recorded = withTimeoutOrNull(10_000) {
            while (incomes().size == before) delay(100)
            incomes()
        }
        val newest = recorded!!.maxBy { it.id }
        assertEquals(1_500_000L, newest.amount)
        assertEquals("Gaji", newest.category?.name)
    }

    @Test
    fun invalidAmount_recordsNothing() = runBlocking<Unit> {
        val salary = money.observeCategories(CategoryType.INCOME).first().first { it.name == "Gaji" }
        val before = incomes().size
        send("lima puluh", salary.id, salary.name)
        delay(2_000)
        assertEquals(before, incomes().size)
    }

    @Test
    fun pick_prefersSalaryAllowanceOther() = runBlocking<Unit> {
        val picked = QuickIncome.pick(money.observeCategories(CategoryType.INCOME).first()).map { it.name }
        assertEquals(listOf("Gaji", "Kiriman", "Lainnya"), picked)
    }
}
