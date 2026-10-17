package com.liezert.itungen.domain.usecase

import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetTest {
    private val food = Category(1, "Makanan", "food", 0, CategoryType.EXPENSE)
    private val bill = Category(2, "Tagihan", BILL_ICON, 0, CategoryType.EXPENSE)
    private val plan = BudgetPlan(income = 3_000_000, cycleStartDay = 1, fixedBills = 500_000, savingsTarget = 500_000)
    private val sep = { day: Int -> LocalDate.of(2026, 9, day) }

    private fun expense(amount: Long, date: LocalDate, category: Category = food) =
        Transaction(0, 1, category, TransactionType.EXPENSE, amount, null, date)

    @Test
    fun specExample_section52() {
        // Pool 3.000.000 − 500.000 − 500.000 − 300.000 = 1.700.000; siklus 30 hari.
        val first = computeBudget(plan, emptyList(), goalsMonthly = 300_000, today = sep(1))
        assertEquals(1_700_000L, first.pool)
        assertEquals(56_000L, first.todayAllowance)

        // Hari ke-11, sudah terpakai 700.000 → 1.000.000 ÷ 20 hari.
        val day11 = computeBudget(plan, listOf(expense(700_000, sep(5))), 300_000, sep(11))
        assertEquals(50_000L, day11.todayAllowance)
        assertEquals(20, day11.daysLeft)
    }

    @Test
    fun todayAllowance_isFrozen_onlyRemainingMoves() {
        val budget = computeBudget(plan, listOf(expense(20_000, sep(11))), 300_000, sep(11))
        assertEquals(85_000L, budget.todayAllowance) // 1.700.000 ÷ 20 hari; belanja hari ini tidak mengubahnya
        assertEquals(65_000L, budget.todayRemaining)
    }

    @Test
    fun bills_useReserve_untilItRunsOut() {
        val paidBill = computeBudget(plan, listOf(expense(500_000, sep(2), bill)), 300_000, sep(3))
        assertEquals(60_000L, paidBill.todayAllowance) // 1.700.000 ÷ 28; tagihan dalam cadangan tidak mengurangi jatah
        val overBill = computeBudget(plan, listOf(expense(800_000, sep(2), bill)), 300_000, sep(3))
        assertEquals(50_000L, overBill.todayAllowance) // kelebihan tagihan 300.000 masuk pengeluaran: 1.400.000 ÷ 28
    }

    @Test
    fun recordedIncomeAbovePlan_growsPool() {
        val bonus = Transaction(0, 1, null, TransactionType.INCOME, 3_600_000, "Gaji + bonus", sep(1))
        assertEquals(2_300_000L, computeBudget(plan, listOf(bonus), 300_000, sep(1)).pool)
    }

    @Test
    fun unrealisticPlan_showsMaxSavings() {
        val tight = plan.copy(savingsTarget = 2_500_000)
        val budget = computeBudget(tight, emptyList(), 300_000, sep(10))
        assertTrue(budget.unrealistic)
        assertEquals(0L, budget.todayAllowance)
        assertEquals(2_200_000L, budget.maxSavings)
    }

    @Test
    fun overspending_isFlaggedAndProjected() {
        val budget = computeBudget(plan, listOf(expense(1_000_000, sep(2))), 300_000, sep(5))
        assertEquals(BudgetStatus.MELEBIHI, budget.status)
        assertTrue(budget.runsOutOn!! < budget.cycleEnd)
    }

    @Test
    fun bigPurchaseOnDayOne_doesNotProjectYet() {
        val budget = computeBudget(plan, listOf(expense(600_000, sep(1))), 300_000, sep(2))
        assertEquals(null, budget.runsOutOn) // baru 1 hari penuh: belum cukup data untuk proyeksi
        assertEquals(BudgetStatus.ON_TRACK, budget.status)
    }

    @Test
    fun morning_isNotHematByDefault() {
        // Hari ke-11 pagi, belanja 10 hari pertama pas sesuai jatah: hari ini yang belum berjalan tidak dihitung.
        val spent = (1..10).map { expense(56_000, sep(it)) }
        assertEquals(BudgetStatus.ON_TRACK, computeBudget(plan, spent, 300_000, sep(11)).status)
    }

    @Test
    fun weekOverspend_isNegative() {
        val budget = computeBudget(plan, listOf(expense(900_000, sep(8))), 300_000, sep(9)) // Rabu
        assertTrue(budget.weekRemaining < 0)
        assertEquals(1_700_000L - 900_000L, budget.cycleRemaining)
    }

    @Test
    fun weeklyPocketMoney_cyclesFromChosenDay() {
        val weekly = BudgetPlan(income = 350_000, cycleStartDay = 3, period = BudgetPeriod.WEEKLY) // Rabu
        val budget = computeBudget(weekly, emptyList(), 0, LocalDate.of(2026, 10, 15)) // Kamis
        assertEquals(LocalDate.of(2026, 10, 14), budget.cycleStart)
        assertEquals(LocalDate.of(2026, 10, 20), budget.cycleEnd)
        assertEquals(58_000L, budget.todayAllowance) // 350.000 ÷ 6 hari tersisa
    }

    @Test
    fun untilNextTransfer_endsTheDayBefore_andFlagsOverdue() {
        val start = LocalDate.of(2026, 10, 10)
        val untilDate = BudgetPlan(
            income = 500_000, cycleStartDay = 1, period = BudgetPeriod.UNTIL_DATE,
            startedOn = start, nextIncomeOn = LocalDate.of(2026, 10, 20),
        )
        val running = computeBudget(untilDate, emptyList(), 0, LocalDate.of(2026, 10, 15))
        assertEquals(LocalDate.of(2026, 10, 19), running.cycleEnd)
        assertEquals(100_000L, running.todayAllowance) // 500.000 ÷ 5 hari
        assertTrue(!running.overdue)
        assertTrue(computeBudget(untilDate, emptyList(), 0, LocalDate.of(2026, 10, 21)).overdue)
    }

    @Test
    fun cycle_wrapsAcrossMonths() {
        val cycle = cycleOf(LocalDate.of(2026, 10, 3), startDay = 25)
        assertEquals(LocalDate.of(2026, 9, 25), cycle.start)
        assertEquals(LocalDate.of(2026, 10, 24), cycle.endInclusive)
    }

    /**
     * Bug A3: rencana dibuat tanggal 17 di siklus 25–24. Dulu seluruh uang sebulan dibagi ke 8 hari tersisa
     * (Rp 275.000/hari), "Sisa minggu ini" lebih besar dari 2 hari jatah, dan status langsung "Hemat".
     */
    @Test
    fun planStartedMidCycle_isProrated() {
        val oct = { day: Int -> LocalDate.of(2026, 10, day) }
        val mid = BudgetPlan(income = 3_000_000, cycleStartDay = 25, fixedBills = 500_000, savingsTarget = 300_000, startedOn = oct(17))
        val budget = computeBudget(mid, emptyList(), 0, oct(17)) // Sabtu
        assertEquals(oct(17), budget.cycleStart)
        assertEquals(8, budget.daysLeft)
        assertEquals(73_000L, budget.todayAllowance) // 2.200.000 × 8/30 hari ÷ 8 hari
        assertTrue(budget.weekAllowance <= budget.todayAllowance * 2) // minggu ini tinggal Sabtu–Minggu
        assertTrue(budget.status != BudgetStatus.HEMAT)

        // Siklus berikutnya penuh lagi seperti biasa.
        assertEquals(70_000L, computeBudget(mid, emptyList(), 0, oct(25)).todayAllowance) // 2.200.000 ÷ 31 hari (25 Okt–24 Nov)
        assertEquals(oct(25), computeBudget(mid, emptyList(), 0, oct(25)).cycleStart)
    }

    /** Bug B3: setoran target yang membuat anggaran minus, bukan tabungan; pesannya harus menunjuk ke sana. */
    @Test
    fun unrealisticPlan_namesTheCause() {
        val goalHeavy = computeBudget(plan.copy(savingsTarget = 300_000), emptyList(), 2_500_000, sep(10))
        assertTrue(goalHeavy.unrealistic)
        assertEquals(Shortfall.GOALS, goalHeavy.shortfall)
        assertEquals(2_500_000L, goalHeavy.goals)
        assertEquals(Shortfall.SAVINGS, computeBudget(plan.copy(savingsTarget = 2_500_000), emptyList(), 300_000, sep(10)).shortfall)
        assertEquals(Shortfall.BILLS, computeBudget(plan.copy(fixedBills = 3_000_000), emptyList(), 0, sep(10)).shortfall)
    }

    /** Bug A4: ganti Bulanan ↔ Mingguan dulu membawa angka apa adanya (3 juta/bulan jadi 3 juta/minggu). */
    @Test
    fun periodSwitch_convertsAmounts() {
        assertEquals(700_000L, convertPerPeriod(3_000_000, fromDays = 30, toDays = 7))
        assertEquals(117_000L, convertPerPeriod(500_000, fromDays = 30, toDays = 7)) // dibulatkan ke ribuan
        assertEquals(3_000_000L, convertPerPeriod(700_000, fromDays = 7, toDays = 30))
        assertEquals(0L, convertPerPeriod(0, fromDays = 30, toDays = 7))
        assertEquals(30L, periodDays(BudgetPeriod.MONTHLY, sep(1), null))
        assertEquals(7L, periodDays(BudgetPeriod.WEEKLY, sep(1), null))
        assertEquals(10L, periodDays(BudgetPeriod.UNTIL_DATE, sep(1), sep(11)))
    }

    /** Untuk warna kalender (B8): jatah harian rata-rata satu siklus penuh dari rencana. */
    @Test
    fun averageDailyAllowance_fromPlan() {
        assertEquals(56_666L, plan.averageDailyAllowance(goalsMonthly = 300_000, day = sep(5))) // 1.700.000 ÷ 30
        assertEquals(null, plan.copy(savingsTarget = 2_500_000).averageDailyAllowance(300_000, sep(5)))
    }
}
