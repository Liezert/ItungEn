package com.liezert.itungen.ui.addtransaction

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.model.Category
import com.liezert.itungen.domain.model.CategoryType
import com.liezert.itungen.domain.model.TransactionError
import com.liezert.itungen.domain.model.TransactionType
import com.liezert.itungen.domain.model.validateTransaction
import com.liezert.itungen.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class AddTransactionUiState(
    val type: CategoryType = CategoryType.EXPENSE,
    val categories: List<Category> = emptyList(),
    val categoryId: Long? = null,
    val date: LocalDate,
    val today: LocalDate,
    val error: TransactionError? = null,
    /** Mengubah transaksi yang sudah ada (bukan mencatat baru). */
    val editing: Boolean = false,
    /** Isi awal nominal & catatan saat mengubah; diterapkan sekali oleh layar setelah dimuat. */
    val initialAmount: Long? = null,
    val initialNote: String? = null,
    val saveFailed: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class AddTransactionViewModel(
    private val repository: TransactionRepository,
    private val clock: Clock,
    saved: SavedStateHandle,
) : ViewModel() {
    /** Id transaksi yang diubah (argumen rute `edit`); null = catat baru. */
    private val editId: Long? = saved.get<Long>(EditArg)?.takeIf { it > 0 }

    // ViewModel mengikuti layar di back stack, jadi form kosong lagi setiap layar dibuka.
    private val form = LocalDate.now(clock).let {
        MutableStateFlow(AddTransactionUiState(date = it, today = it, editing = editId != null))
    }

    init {
        if (editId != null) viewModelScope.launch {
            val tx = repository.getTransaction(editId)
            if (tx == null) {
                form.update { it.copy(saved = true) } // sudah dihapus (mis. di HP lain): tutup saja
                return@launch
            }
            form.update {
                it.copy(
                    type = if (tx.type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE,
                    categoryId = tx.category?.id,
                    date = tx.date,
                    initialAmount = tx.amount,
                    initialNote = tx.note,
                )
            }
        }
    }

    val uiState: StateFlow<AddTransactionUiState> = combine(
        form,
        repository.observeCategories(CategoryType.EXPENSE),
        repository.observeCategories(CategoryType.INCOME),
    ) { form, expense, income ->
        val categories = if (form.type == CategoryType.EXPENSE) expense else income
        // Kategori pertama terpilih sejak awal, seperti di Figma.
        form.copy(categories = categories, categoryId = form.categoryId ?: categories.firstOrNull()?.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    fun selectType(type: CategoryType) = form.update {
        // Kategori beda per tipe, jadi pilihan lama tidak berlaku lagi.
        if (it.type == type) it else it.copy(type = type, categoryId = null, error = null)
    }

    fun selectCategory(id: Long) = form.update { it.copy(categoryId = id, error = null) }

    fun selectDate(date: LocalDate) = form.update { it.copy(date = date, error = null) }


    /** [amountDigits] berisi angka saja, tanpa pemisah ribuan. */
    fun save(amountDigits: String, note: String) {
        val current = uiState.value
        if (current.saving) return
        val amount = amountDigits.toLongOrNull()
        val error = validateTransaction(amount, current.categoryId, current.date, LocalDate.now(clock))
        if (error != null) {
            form.update { it.copy(error = error) }
            return
        }
        form.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            val type = if (current.type == CategoryType.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME
            val cleanNote = note.trim().ifEmpty { null }
            try {
                if (editId != null) {
                    repository.updateTransaction(editId, type, amount!!, current.categoryId!!, cleanNote, current.date)
                } else {
                    repository.addToMainWallet(type, amount!!, current.categoryId!!, cleanNote, current.date)
                }
                form.update { it.copy(saving = false, saved = true) }
            } catch (_: SQLiteException) {
                // Jangan crash; isian tetap ada agar pengguna bisa coba lagi (§7.3).
                form.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    companion object {
        /** Argumen rute: id transaksi yang diubah. */
        const val EditArg = "edit"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ItungEnApp).container
                AddTransactionViewModel(container.transactionRepository, container.clock, createSavedStateHandle())
            }
        }
    }
}
