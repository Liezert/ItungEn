package com.liezert.itungen.ui.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.domain.usecase.CategorySpend
import com.liezert.itungen.domain.usecase.categorySpending
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

data class InsightUiState(
    val loading: Boolean = true,
    /** Pengeluaran bulan ini per kategori, terbesar dulu. */
    val spending: List<CategorySpend> = emptyList(),
) {
    val total: Long get() = spending.sumOf { it.total }
}

class InsightViewModel(repository: TransactionRepository, clock: Clock) : ViewModel() {
    val uiState: StateFlow<InsightUiState> = repository.observeTransactions().map { transactions ->
        InsightUiState(loading = false, spending = categorySpending(transactions, YearMonth.from(LocalDate.now(clock))))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ItungEnApp).container
                InsightViewModel(container.transactionRepository, container.clock)
            }
        }
    }
}
