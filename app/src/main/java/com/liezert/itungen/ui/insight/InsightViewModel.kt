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

    // Total per kategori bulan ini dan bulan lalu langsung dari SQL (GROUP BY), bukan dari seluruh riwayat.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InsightUiState> = combine(month, type) { m, t -> m to t }.flatMapLatest { (m, t) ->
        val txType = if (t == CategoryType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
        val previousMonth = m.minusMonths(1)
        combine(
            repository.observeCategoryTotals(txType, m.atDay(1), m.atEndOfMonth()),
            repository.observeCategoryTotals(txType, previousMonth.atDay(1), previousMonth.atEndOfMonth()),
            repository.observeFirstDate(),
        ) { spending, previous, firstDate ->
            val first = firstDate?.let(YearMonth::from) ?: m
            InsightUiState(
                month = m,
                type = t,
                loading = false,
                spending = spending,
                previous = previous.associate { it.category.id to it.total },
                canGoBack = m > first,
                canGoForward = m < YearMonth.now(clock),
            )
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightUiState(YearMonth.now(clock)))

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
