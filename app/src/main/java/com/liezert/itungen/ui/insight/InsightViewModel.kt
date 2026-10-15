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
import com.liezert.itungen.domain.usecase.CategorySpend
import com.liezert.itungen.domain.usecase.categorySpending
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
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
) {
    val total: Long get() = spending.sumOf { it.total }
    val previousTotal: Long get() = previous.values.sum()
}

class InsightViewModel(repository: TransactionRepository, private val clock: Clock) : ViewModel() {
    private val month = MutableStateFlow(YearMonth.now(clock))
    private val type = MutableStateFlow(CategoryType.EXPENSE)

    val uiState: StateFlow<InsightUiState> = combine(repository.observeTransactions(), month, type) { transactions, m, t ->
        val first = transactions.minOfOrNull { YearMonth.from(it.date) } ?: m
        val txType = if (t == CategoryType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
        InsightUiState(
            month = m,
            type = t,
            loading = false,
            spending = categorySpending(transactions, m, txType),
            previous = categorySpending(transactions, m.minusMonths(1), txType).associate { it.category.id to it.total },
            canGoBack = m > first,
            canGoForward = m < YearMonth.now(clock),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightUiState(YearMonth.now(clock)))

    fun selectType(value: CategoryType) {
        type.value = value
    }

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { if (it < YearMonth.now(clock)) it.plusMonths(1) else it }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ItungEnApp).container
                InsightViewModel(container.transactionRepository, container.clock)
            }
        }
    }
}
