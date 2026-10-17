package com.liezert.itungen.data.demo

import com.liezert.itungen.R
import com.liezert.itungen.AppContainer
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.usecase.BudgetPlan
import com.liezert.itungen.domain.usecase.cycleOf
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private const val Payday = 25

/**
 * Data contoh untuk mencoba app tanpa mencatat dulu (mis. pengulas portofolio): ±6 minggu catatan harian, gaji
 * tanggal 25, satu target tabungan, dan anggaran bulanan. Hanya ditawarkan saat belum ada transaksi.
 */
suspend fun seedDemoData(container: AppContainer) {
    val repo = container.transactionRepository
    val text = container.appContext
    val today = LocalDate.now(container.clock)
    val expense = repo.observeCategories(CategoryType.EXPENSE).first().associate { it.icon to it.id }
    val income = repo.observeCategories(CategoryType.INCOME).first().associate { it.icon to it.id }
    suspend fun spend(icon: String, amount: Long, note: String, date: LocalDate) =
        repo.addToMainWallet(TransactionType.EXPENSE, amount, expense.getValue(icon), note, date)

    val from = today.minusDays(42)
    var payday = cycleOf(from, Payday).start
    while (payday <= today) {
        if (payday >= from) repo.addToMainWallet(TransactionType.INCOME, 3_500_000, income.getValue("salary"), text.getString(R.string.cat_salary), payday)
        payday = payday.plusMonths(1)
    }
    repo.addToMainWallet(TransactionType.INCOME, 2_000_000, income.getValue("other"), text.getString(R.string.demo_opening_balance), from)
    for (daysAgo in 42 downTo 0) {
        val date = today.minusDays(daysAgo.toLong())
        spend("food", 15_000L + daysAgo % 3 * 2_000, text.getString(R.string.demo_lunch), date)
        if (daysAgo % 2 == 0) spend("transport", 12_000, text.getString(R.string.demo_ride), date)
        if (daysAgo % 7 == 5) spend("entertainment", 50_000, text.getString(R.string.demo_movie), date)
        if (daysAgo % 10 == 3) spend("shopping", 120_000, text.getString(R.string.demo_groceries), date)
        if (date.dayOfMonth == 27) spend("bill", 750_000, text.getString(R.string.demo_rent), date)
    }

    val goal = container.goalRepository.createGoal(text.getString(R.string.demo_goal), 1_200_000, today.plusMonths(4), 300_000)
    container.goalRepository.addFunds(goal, 450_000)
    if (container.budgetPlan.value == null) {
        container.saveBudgetPlan(BudgetPlan(income = 3_500_000, cycleStartDay = Payday, fixedBills = 750_000, savingsTarget = 300_000))
    }
}
