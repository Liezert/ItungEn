package com.liezert.itungen.domain.usecase

import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.model.TransactionType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Kapan uang masuk: gaji bulanan, uang saku mingguan, atau kiriman tidak tentu (tanggal berikutnya diisi manual). */
enum class BudgetPeriod { MONTHLY, WEEKLY, UNTIL_DATE }

/**
 * Rencana yang diisi sekali di awal (Pengaturan → Anggaran). Tanpa gaji otomatis (§4.1 dihapus), [income] adalah
 * perkiraan uang masuk per periode: pemasukan yang benar-benar tercatat dipakai bila lebih besar.
 * Semua nominal per periode (per bulan, per minggu, atau sampai kiriman berikutnya).
 */
data class BudgetPlan(
    val income: Long,
    /** MONTHLY: tanggal 1–28 (ada di setiap bulan). WEEKLY: hari 1 = Senin … 7 = Minggu. UNTIL_DATE: tidak dipakai. */
    val cycleStartDay: Int,
    /** Tagihan tetap per periode, dicadangkan di awal siklus (§5.1). */
    val fixedBills: Long = 0,
    val savingsTarget: Long = 0,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
    /** UNTIL_DATE: siklus = [startedOn] sampai sehari sebelum [nextIncomeOn]. */
    val startedOn: LocalDate? = null,
    val nextIncomeOn: LocalDate? = null,
)

enum class BudgetStatus { HEMAT, ON_TRACK, MELEBIHI }

data class Budget(
    val cycleStart: LocalDate,
    val cycleEnd: LocalDate,
    /** Uang yang boleh dipakai untuk pengeluaran fleksibel di siklus ini. */
    val pool: Long,
    /** Dibekukan di awal hari (§5.2): dihitung dari kondisi sebelum hari ini. */
    val todayAllowance: Long,
    /** Boleh negatif = sudah lewat jatah hari ini. */
    val todayRemaining: Long,
    val weekAllowance: Long,
    /** Boleh negatif = minggu ini sudah lewat jatah. */
    val weekRemaining: Long,
    /** Sisa uang fleksibel sampai akhir siklus; boleh negatif. */
    val cycleRemaining: Long,
    /** Sisa hari siklus termasuk hari ini. */
    val daysLeft: Int,
    val status: BudgetStatus,
    /** Tanggal uang fleksibel habis bila pola belanja sekarang berlanjut; null = cukup, atau data belum cukup. */
    val runsOutOn: LocalDate?,
    /** Pool ≤ 0: target/tagihan tidak realistis; [maxSavings] = tabungan maksimal yang masih mungkin (§5.3). */
    val unrealistic: Boolean,
    val maxSavings: Long,
    /** UNTIL_DATE: tanggal kiriman sudah lewat, pengguna perlu mengisi tanggal berikutnya. */
    val overdue: Boolean = false,
    val period: BudgetPeriod = BudgetPeriod.MONTHLY,
)

/** Kategori yang dianggap tagihan tetap: pengeluarannya mengurangi cadangan tagihan, bukan jatah harian. */
const val BILL_ICON = "bill"

/** Proyeksi dan status tren baru dihitung setelah sekian hari penuh, agar satu belanja besar di awal tidak memicu panik. */
private const val MinFullDays = 3

/** Siklus bulanan yang memuat [today]: dari tanggal [startDay] sampai sehari sebelum tanggal yang sama bulan depan. */
fun cycleOf(today: LocalDate, startDay: Int): ClosedRange<LocalDate> {
    val day = startDay.coerceIn(1, 28)
    val start = if (today.dayOfMonth >= day) today.withDayOfMonth(day) else today.minusMonths(1).withDayOfMonth(day)
    return start..start.plusMonths(1).minusDays(1)
}

/** Siklus rencana yang memuat [today]. UNTIL_DATE yang sudah lewat diperpanjang sampai hari ini. */
fun BudgetPlan.cycleOf(today: LocalDate): ClosedRange<LocalDate> = when (period) {
    BudgetPeriod.MONTHLY -> cycleOf(today, cycleStartDay)
    BudgetPeriod.WEEKLY -> {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(cycleStartDay.coerceIn(1, 7))))
        start..start.plusDays(6)
    }
    BudgetPeriod.UNTIL_DATE -> {
        val start = minOf(startedOn ?: today, today)
        val end = (nextIncomeOn ?: today.plusDays(1)).minusDays(1)
        start..maxOf(end, today)
    }
}

/**
 * Mesin jatah harian (§5.2): rolling — boros kemarin menurunkan jatah hari ini, hemat menaikkannya.
 * [goalsMonthly] = jumlah setoran rencana goal aktif per bulan (disesuaikan dengan panjang siklus).
 * Pembulatan ke bawah ke Rp 1.000.
 */
fun computeBudget(plan: BudgetPlan, transactions: List<Transaction>, goalsMonthly: Long, today: LocalDate): Budget {
    val cycle = plan.cycleOf(today)
    val start = cycle.start
    val end = cycle.endInclusive
    val inCycle = transactions.filter { it.date in start..today }
    val totalDays = (ChronoUnit.DAYS.between(start, end) + 1).toInt()
    val goals = if (plan.period == BudgetPeriod.MONTHLY) goalsMonthly else goalsMonthly * totalDays / 30

    val recordedIncome = inCycle.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val income = maxOf(recordedIncome, plan.income)
    val pool = income - plan.fixedBills - plan.savingsTarget - goals
    val overdue = plan.period == BudgetPeriod.UNTIL_DATE && (plan.nextIncomeOn == null || plan.nextIncomeOn <= today)

    // Pengeluaran fleksibel s.d. [day] (inklusif): bukan tagihan, ditambah tagihan yang melebihi cadangannya.
    fun spentThrough(day: LocalDate): Long {
        val expenses = inCycle.filter { it.type == TransactionType.EXPENSE && it.date <= day }
        val (bills, flexible) = expenses.partition { it.category?.icon == BILL_ICON }
        return flexible.sumOf { it.amount } + maxOf(0, bills.sumOf { it.amount } - plan.fixedBills)
    }

    val daysLeft = (ChronoUnit.DAYS.between(today, end) + 1).toInt().coerceAtLeast(1)
    val fullDays = totalDays - daysLeft // hari siklus yang sudah lewat penuh (hari ini belum selesai)
    val spentBeforeToday = spentThrough(today.minusDays(1))
    val spentToday = spentThrough(today) - spentBeforeToday
    val spentTotal = spentBeforeToday + spentToday

    if (pool <= 0) {
        return Budget(
            start, end, pool, 0, -spentToday, 0, 0, pool - spentTotal, daysLeft, BudgetStatus.MELEBIHI, null,
            unrealistic = true, maxSavings = maxOf(0, income - plan.fixedBills - goals), overdue = overdue, period = plan.period,
        )
    }

    val todayAllowance = floor1000(maxOf(0, pool - spentBeforeToday) / daysLeft)

    val weekFrom = maxOf(weekStart(today), start)
    val weekTo = minOf(weekStart(today).plusDays(6), end) // Minggu, dipotong akhir siklus
    val daysAtWeekStart = ChronoUnit.DAYS.between(weekFrom, end) + 1
    val weekDays = ChronoUnit.DAYS.between(weekFrom, weekTo) + 1
    val spentBeforeWeek = spentThrough(weekFrom.minusDays(1))
    val weekAllowance = floor1000(maxOf(0, pool - spentBeforeWeek) * weekDays / daysAtWeekStart)

    // Tren dibandingkan dengan hari yang sudah selesai saja: pagi hari tidak otomatis terlihat "Hemat".
    val enoughData = fullDays >= MinFullDays
    val expectedBefore = pool * fullDays / totalDays
    val status = when {
        spentTotal >= pool || spentToday > todayAllowance -> BudgetStatus.MELEBIHI
        enoughData && spentBeforeToday > expectedBefore * 11 / 10 -> BudgetStatus.MELEBIHI
        enoughData && spentBeforeToday < expectedBefore * 85 / 100 -> BudgetStatus.HEMAT
        else -> BudgetStatus.ON_TRACK
    }
    val dailyAverage = if (enoughData) spentBeforeToday / fullDays else 0
    val runsOutOn = if (dailyAverage <= 0) null else {
        val lastsDays = maxOf(0, pool - spentTotal) / dailyAverage
        today.plusDays(lastsDays).takeIf { it < end }
    }

    return Budget(
        cycleStart = start,
        cycleEnd = end,
        pool = pool,
        todayAllowance = todayAllowance,
        todayRemaining = todayAllowance - spentToday,
        weekAllowance = weekAllowance,
        weekRemaining = weekAllowance - (spentTotal - spentBeforeWeek),
        cycleRemaining = pool - spentTotal,
        daysLeft = daysLeft,
        status = status,
        runsOutOn = runsOutOn,
        unrealistic = false,
        maxSavings = 0,
        overdue = overdue,
        period = plan.period,
    )
}

private fun floor1000(amount: Long) = amount / 1_000 * 1_000
