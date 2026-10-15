package com.liezert.itungen.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.domain.usecase.filterNames
import com.liezert.itungen.ui.components.UndoStack
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val today: LocalDate,
    val zone: ZoneId,
    val loading: Boolean = true,
    /** Terbaru dulu. */
    val transactions: List<Transaction> = emptyList(),
    val filters: List<String> = emptyList(),
    val monthCount: Int = 0,
)

class TransactionsViewModel(
    private val repository: TransactionRepository,
    clock: Clock,
    /** Lingkup aplikasi: penghapusan tetap terjadi walau layar ditinggalkan sebelum waktu Urungkan habis. */
    private val appScope: CoroutineScope,
) : ViewModel() {
    /** Langsung terhapus dari database (aman walau app ditutup); Urungkan memulihkan baris yang sama. */
    val undo = UndoStack(viewModelScope, appScope)

    val uiState: StateFlow<TransactionsUiState> = combine(
        repository.observeTransactions(),
        repository.observeCategories(CategoryType.EXPENSE),
        repository.observeCategories(CategoryType.INCOME),
    ) { transactions, expense, income ->
        val today = LocalDate.now(clock)
        val month = YearMonth.from(today)
        TransactionsUiState(
            today = today,
            zone = clock.zone,
            loading = false,
            transactions = transactions,
            filters = filterNames(expense, income),
            monthCount = transactions.count { YearMonth.from(it.date) == month },
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TransactionsUiState(today = LocalDate.now(clock), zone = clock.zone),
    )

    fun delete(tx: Transaction) {
        appScope.launch {
            val restore = repository.deleteTransaction(tx.id)
            undo.push("Dihapus: ${tx.note ?: tx.category?.name ?: "Transaksi"}", restore)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ItungEnApp).container
                TransactionsViewModel(container.transactionRepository, container.clock, container.appScope)
            }
        }
    }
}
