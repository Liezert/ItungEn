package com.liezert.itungen.ui.transactions

import com.liezert.itungen.ui.components.displayNote
import com.liezert.itungen.ui.components.displayName
import android.content.Context
import com.liezert.itungen.R
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.Transaction
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.domain.usecase.filterNames
import com.liezert.itungen.ui.components.UndoStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Baris yang dimuat per langkah saat daftar discroll ke bawah. */
private const val PageSize = 200

data class TransactionsUiState(
    val today: LocalDate,
    val zone: ZoneId,
    val loading: Boolean = true,
    /** Hasil cari/filter, terbaru dulu, paling banyak sebanyak yang sudah dimuat. */
    val transactions: List<Transaction> = emptyList(),
    val filters: List<String> = emptyList(),
    val monthCount: Int = 0,
    val query: String = "",
    /** null = "Semua". */
    val category: String? = null,
    /** Masih ada baris lebih lama yang belum dimuat. */
    val canLoadMore: Boolean = false,
    /** Ada transaksi sama sekali (membedakan "belum ada" dari "tidak ada yang cocok"). */
    val hasAny: Boolean = false,
)

class TransactionsViewModel(
    private val repository: TransactionRepository,
    clock: Clock,
    /** Lingkup aplikasi: penghapusan & Urungkan tetap selesai walau layar ditinggalkan. */
    private val appScope: CoroutineScope,
    private val saved: SavedStateHandle,
    /** Teks pesan Urungkan dalam bahasa app. */
    private val context: Context,
) : ViewModel() {
    /** Langsung terhapus dari database (aman walau app ditutup); Urungkan memulihkan baris yang sama. */
    val undo = UndoStack(viewModelScope, appScope)

    private val limit = MutableStateFlow(PageSize)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TransactionsUiState> = combine(
        saved.getStateFlow(QueryKey, ""),
        saved.getStateFlow<String?>(CategoryKey, null),
        limit,
    ) { query, category, limit -> Triple(query, category, limit) }
        .flatMapLatest { (query, category, limit) ->
            val today = LocalDate.now(clock)
            val month = YearMonth.from(today)
            combine(
                repository.observeSearch(query, category, limit),
                repository.observeTotals(month.atDay(1), month.atEndOfMonth()),
                repository.observeCategories(CategoryType.EXPENSE),
                repository.observeCategories(CategoryType.INCOME),
                repository.observeHasTransactions(),
            ) { rows, totals, expense, income, hasAny ->
                TransactionsUiState(
                    today = today,
                    zone = clock.zone,
                    loading = false,
                    transactions = rows,
                    filters = filterNames(expense, income),
                    monthCount = totals.count,
                    query = query,
                    category = category,
                    canLoadMore = rows.size == limit,
                    hasAny = hasAny,
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TransactionsUiState(today = LocalDate.now(clock), zone = clock.zone),
        )

    fun search(query: String) {
        saved[QueryKey] = query
        limit.value = PageSize
    }

    fun filter(category: String?) {
        saved[CategoryKey] = category
        limit.value = PageSize
    }

    /** Dipanggil saat daftar mendekati baris terakhir yang sudah dimuat. */
    fun loadMore() {
        if (uiState.value.canLoadMore) limit.update { it + PageSize }
    }

    fun delete(tx: Transaction) {
        appScope.launch {
            val restore = repository.deleteTransaction(tx.id)
            val label = tx.displayNote(context) ?: tx.category?.displayName(context) ?: context.getString(R.string.transaction)
            undo.push(context.getString(R.string.undo_deleted, label), restore)
        }
    }

    companion object {
        private const val QueryKey = "query"
        private const val CategoryKey = "category"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ItungEnApp).container
                TransactionsViewModel(container.transactionRepository, container.clock, container.appScope, createSavedStateHandle(), this[APPLICATION_KEY] as ItungEnApp)
            }
        }
    }
}
