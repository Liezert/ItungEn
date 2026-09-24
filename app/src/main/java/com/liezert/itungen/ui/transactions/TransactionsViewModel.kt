package com.liezert.itungen.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class TransactionsUiState(
    val loading: Boolean = true,
    val balance: Long = 0,
    /** Dikelompokkan per hari, terbaru dulu. */
    val days: List<Pair<LocalDate, List<Transaction>>> = emptyList(),
)

class TransactionsViewModel(repository: TransactionRepository) : ViewModel() {
    val uiState: StateFlow<TransactionsUiState> =
        combine(repository.observeMainBalance(), repository.observeTransactions()) { balance, transactions ->
            TransactionsUiState(
                loading = false,
                balance = balance,
                days = transactions.groupBy { it.date }.toList(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer { TransactionsViewModel((this[APPLICATION_KEY] as ItungEnApp).container.transactionRepository) }
        }
    }
}
