package com.liezert.itungen.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.GoalStatus
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.repository.GoalRepository
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.domain.usecase.Budget
import com.liezert.itungen.domain.usecase.BudgetPlan
import com.liezert.itungen.domain.usecase.QuickTemplate
import com.liezert.itungen.domain.usecase.computeBudget
import com.liezert.itungen.domain.usecase.cycleOf
import com.liezert.itungen.domain.usecase.quickTemplates
import com.liezert.itungen.ui.components.UndoStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class HomeUiState(
    val today: LocalDate,
    val zone: ZoneId,
    val loading: Boolean = true,
    val balance: Long = 0,
    val monthIncome: Long = 0,
    val monthExpense: Long = 0,
    /** null = anggaran belum diatur: Beranda menampilkan ajakan mengaturnya. */
    val budget: Budget? = null,
    /** Riwayat terakhir, terbaru dulu. */
    val recent: List<Transaction> = emptyList(),
    val templates: List<QuickTemplate> = emptyList(),
    /** Target yang ditampilkan di Beranda. */
    val goal: Goal? = null,
)

/** Jumlah baris "Riwayat Terakhir" di Beranda. */
const val RecentCount = 4

/** Jendela catat cepat: kebiasaan 60 hari terakhir. */
private const val TemplateDays = 60L

class HomeViewModel(
    private val repository: TransactionRepository,
    goals: GoalRepository,
    budgetPlan: Flow<BudgetPlan?>,
    private val clock: Clock,
    /** Lingkup aplikasi: Urungkan tetap bekerja walau layar ditinggalkan. */
    private val appScope: CoroutineScope,
) : ViewModel() {
    // Tidak memuat seluruh riwayat: total bulan ini dari SQL, plus satu jendela tanggal (siklus anggaran dan
    // 60 hari terakhir untuk catat cepat) yang dihitung di Dispatchers.Default.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = budgetPlan.flatMapLatest { plan ->
        val today = LocalDate.now(clock)
        val month = YearMonth.from(today)
        val windowStart = minOf(today.minusDays(TemplateDays), plan?.cycleOf(today)?.start ?: today)
        combine(
            repository.observeMainBalance(),
            repository.observeTotals(month.atDay(1), month.atEndOfMonth()),
            repository.observeBetween(windowStart, today),
            repository.observeRecent(RecentCount),
            goals.observeGoals(),
        ) { balance, totals, window, recent, goalList ->
            val goalsMonthly = goalList.filter { it.status == GoalStatus.ACTIVE }.sumOf { it.plannedMonthly }
            HomeUiState(
                today = today,
                zone = clock.zone,
                loading = false,
                balance = balance,
                monthIncome = totals.income,
                monthExpense = totals.expense,
                budget = plan?.let { computeBudget(it, window, goalsMonthly, today) },
                recent = recent,
                templates = quickTemplates(window, today),
                // Target yang masih dikejar lebih berguna di Beranda daripada yang sudah tercapai.
                goal = goalList.firstOrNull { it.status == GoalStatus.ACTIVE } ?: goalList.firstOrNull(),
            )
        }
    }.flowOn(Dispatchers.Default).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(today = LocalDate.now(clock), zone = clock.zone),
    )

    /** Catatan cepat yang baru dibuat; masing-masing bisa diurungkan beberapa detik. */
    val undo = UndoStack(viewModelScope, appScope)
    private var lastQuickAddAt = 0L

    /** Catat template hari ini dengan sekali ketuk. Ketukan ganda (< 1 detik) diabaikan. */
    fun quickAdd(template: QuickTemplate) {
        val now = System.nanoTime()
        if (now - lastQuickAddAt < 1_000_000_000L) return
        lastQuickAddAt = now
        appScope.launch {
            val id = repository.addToMainWallet(template.type, template.amount, template.categoryId, template.note, LocalDate.now(clock))
            undo.push("Tercatat: ${template.label}") { repository.deleteTransaction(id) }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as ItungEnApp).container
                HomeViewModel(c.transactionRepository, c.goalRepository, c.budgetPlan, c.clock, c.appScope)
            }
        }
    }
}
