package com.liezert.itungen.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.model.GoalStatus
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.repository.GoalRepository
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.domain.usecase.expenseBetween
import com.liezert.itungen.domain.usecase.filterNames
import com.liezert.itungen.domain.usecase.monthNet
import com.liezert.itungen.domain.usecase.weekStart
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class HomeUiState(
    val today: LocalDate,
    val zone: ZoneId,
    val loading: Boolean = true,
    val balance: Long = 0,
    /** Pemasukan dikurangi pengeluaran bulan ini. */
    val monthNet: Long = 0,
    /** Pengeluaran sejak Senin minggu ini. */
    val weekSpent: Long = 0,
    /** Terbaru dulu. */
    val transactions: List<Transaction> = emptyList(),
    val filters: List<String> = emptyList(),
    /** Target yang ditampilkan di Beranda. */
    val goal: Goal? = null,
)

class HomeViewModel(repository: TransactionRepository, goals: GoalRepository, clock: Clock) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeMainBalance(),
        repository.observeTransactions(),
        repository.observeCategories(CategoryType.EXPENSE),
        repository.observeCategories(CategoryType.INCOME),
        goals.observeGoals(),
    ) { balance, transactions, expense, income, goalList ->
        val today = LocalDate.now(clock)
        HomeUiState(
            today = today,
            zone = clock.zone,
            loading = false,
            balance = balance,
            monthNet = monthNet(transactions, YearMonth.from(today)),
            weekSpent = expenseBetween(transactions, weekStart(today), today),
            transactions = transactions,
            filters = filterNames(expense, income),
            // Target yang masih dikejar lebih berguna di Beranda daripada yang sudah tercapai.
            goal = goalList.firstOrNull { it.status == GoalStatus.ACTIVE } ?: goalList.firstOrNull(),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState(today = LocalDate.now(clock), zone = clock.zone),
    )

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ItungEnApp).container
                HomeViewModel(container.transactionRepository, container.goalRepository, container.clock)
            }
        }
    }
}
