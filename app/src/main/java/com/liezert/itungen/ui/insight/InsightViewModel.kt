package com.liezert.itungen.ui.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.data.settings.SyncedSettings
import com.liezert.itungen.domain.usecase.CategorySpend
import com.liezert.itungen.domain.usecase.recordStreak
import com.liezert.itungen.domain.usecase.BudgetPlan
import com.liezert.itungen.domain.usecase.averageDailyAllowance
import com.liezert.itungen.domain.usecase.canMarkNoSpend
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

data class InsightUiState(
    val month: YearMonth,
    /** Pengeluaran atau pemasukan yang sedang ditampilkan. */
    val type: CategoryType = CategoryType.EXPENSE,
    val loading: Boolean = true,
    /** Total [type] pada [month] per kategori, terbesar dulu. */
    val spending: List<CategorySpend> = emptyList(),
    /** Total [type] bulan sebelumnya per id kategori, untuk perbandingan. */
    val previous: Map<Long, Long> = emptyMap(),
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val today: LocalDate = LocalDate.MIN,
    /** Batas bulanan per uuid kategori (hanya berlaku untuk pengeluaran). */
    val limits: Map<String, Long> = emptyMap(),
    /** Pengeluaran per hari pada [month] (kalender). */
    val daily: Map<LocalDate, Long> = emptyMap(),
    val noSpendDays: Set<LocalDate> = emptySet(),
    /** Hari tercatat berturut-turut sampai hari ini. */
    val streak: Int = 0,
    /** Hari yang diketuk di kalender. */
    val selectedDay: LocalDate? = null,
    /** Jatah harian rata-rata dari rencana anggaran, ukuran warna kalender; null = belum ada anggaran. */
    val dailyAllowance: Long? = null,
) {
    val total: Long get() = spending.sumOf { it.total }
    val previousTotal: Long get() = previous.values.sum()
}

/** Streak dihitung dari hari tercatat setahun terakhir. */
private const val StreakWindowDays = 400L

private class Habits(
    val daily: Map<LocalDate, Long>,
    val recorded: Set<LocalDate>,
    val noSpend: Set<LocalDate>,
    val limits: Map<String, Long>,
    val selected: LocalDate?,
)

class InsightViewModel(
    repository: TransactionRepository,
    private val settings: SyncedSettings,
    private val clock: Clock,
    budgetPlan: Flow<BudgetPlan?> = flowOf(null),
    goals: Flow<List<Goal>> = flowOf(emptyList()),
) : ViewModel() {
    private val allowance = combine(budgetPlan, goals) { plan, list ->
        val goalsMonthly = list.filter { it.status == GoalStatus.ACTIVE }.sumOf { it.plannedMonthly }
        plan?.averageDailyAllowance(goalsMonthly, LocalDate.now(clock))
    }
    private val month = MutableStateFlow(YearMonth.now(clock))
    private val type = MutableStateFlow(CategoryType.EXPENSE)
    private val selectedDay = MutableStateFlow<LocalDate?>(null)

    // Total per kategori bulan ini dan bulan lalu langsung dari SQL (GROUP BY), bukan dari seluruh riwayat.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InsightUiState> = combine(month, type) { m, t -> m to t }.flatMapLatest { (m, t) ->
        val txType = if (t == CategoryType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
        val previousMonth = m.minusMonths(1)
        val today = LocalDate.now(clock)
        // Kalender & streak: pengeluaran harian bulan ini dan hari tercatat setahun terakhir (cukup untuk streak).
        val habits = combine(
            repository.observeDailyExpense(m.atDay(1), m.atEndOfMonth()),
            repository.observeRecordedDays(today.minusDays(StreakWindowDays), today),
            settings.noSpendDays,
            settings.categoryLimits,
            selectedDay,
        ) { daily, recorded, noSpend, limits, selected -> Habits(daily, recorded + noSpend, noSpend, limits, selected) }
        combine(
            repository.observeCategoryTotals(txType, m.atDay(1), m.atEndOfMonth()),
            repository.observeCategoryTotals(txType, previousMonth.atDay(1), previousMonth.atEndOfMonth()),
            repository.observeFirstDate(),
            habits,
            allowance,
        ) { spending, previous, firstDate, h, dailyAllowance ->
            val first = firstDate?.let(YearMonth::from) ?: m
            InsightUiState(
                month = m,
                type = t,
                loading = false,
                spending = spending,
                previous = previous.associate { it.category.id to it.total },
                canGoBack = m > first,
                canGoForward = m < YearMonth.now(clock),
                today = today,
                limits = h.limits,
                daily = h.daily,
                noSpendDays = h.noSpend,
                streak = recordStreak(h.recorded, today),
                selectedDay = h.selected?.takeIf { YearMonth.from(it) == m },
                dailyAllowance = dailyAllowance,
            )
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightUiState(YearMonth.now(clock)))

    fun selectDay(day: LocalDate?) {
        selectedDay.value = day
    }

    /** Tandai "tidak jajan" (hari ini atau kemarin saja, lihat [canMarkNoSpend]) atau batalkan tanda hari mana pun. */
    fun toggleNoSpend(day: LocalDate) {
        val mark = day !in settings.noSpendDays.value
        if (mark && !canMarkNoSpend(day, LocalDate.now(clock))) return
        settings.setNoSpendDay(day, marked = mark)
    }

    /** [limit] null = hapus batas. */
    fun setLimit(categoryUuid: String, limit: Long?) = settings.setCategoryLimit(categoryUuid, limit)

    fun selectType(value: CategoryType) {
        type.value = value
    }

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { if (it < YearMonth.now(clock)) it.plusMonths(1) else it }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ItungEnApp).container
                InsightViewModel(
                    container.transactionRepository, container.settings, container.clock,
                    container.budgetPlan, container.goalRepository.observeGoals(),
                )
            }
        }
    }
}
