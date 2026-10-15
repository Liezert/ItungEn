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
import com.liezert.itungen.domain.usecase.monthExpense
import com.liezert.itungen.domain.usecase.monthIncome
import com.liezert.itungen.domain.usecase.quickTemplates
import com.liezert.itungen.ui.components.UndoStack
import kotlinx.coroutines.CoroutineScope
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
    /** Terbaru dulu. */
    val transactions: List<Transaction> = emptyList(),
    val templates: List<QuickTemplate> = emptyList(),
    /** Target yang ditampilkan di Beranda. */
    val goal: Goal? = null,
)

class HomeViewModel(
    private val repository: TransactionRepository,
    goals: GoalRepository,
    budgetPlan: Flow<BudgetPlan?>,
    private val clock: Clock,
    /** Lingkup aplikasi: Urungkan tetap bekerja walau layar ditinggalkan. */
    private val appScope: CoroutineScope,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeMainBalance(),
        repository.observeTransactions(),
        goals.observeGoals(),
        budgetPlan,
    ) { balance, transactions, goalList, plan ->
        val today = LocalDate.now(clock)
        val month = YearMonth.from(today)
        val goalsMonthly = goalList.filter { it.status == GoalStatus.ACTIVE }.sumOf { it.plannedMonthly }
        HomeUiState(
            today = today,
            zone = clock.zone,
            loading = false,
            balance = balance,
            monthIncome = monthIncome(transactions, month),
            monthExpense = monthExpense(transactions, month),
            budget = plan?.let { computeBudget(it, transactions, goalsMonthly, today) },
            transactions = transactions,
            templates = quickTemplates(transactions, today),
            // Target yang masih dikejar lebih berguna di Beranda daripada yang sudah tercapai.
            goal = goalList.firstOrNull { it.status == GoalStatus.ACTIVE } ?: goalList.firstOrNull(),
        )
    }.stateIn(
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
