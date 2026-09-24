package com.liezert.itungen.ui.addtransaction

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
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
    /** Gaji / tagihan tetap: dicatat otomatis setiap bulan pada tanggal yang sama. */
    val repeatMonthly: Boolean = false,
    val error: TransactionError? = null,
    val saveFailed: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class AddTransactionViewModel(
    private val repository: TransactionRepository,
    private val clock: Clock,
) : ViewModel() {

    private fun initialState() = LocalDate.now(clock).let { AddTransactionUiState(date = it, today = it) }

    private val form = MutableStateFlow(initialState())

    val uiState: StateFlow<AddTransactionUiState> = combine(
        form,
        repository.observeCategories(CategoryType.EXPENSE),
        repository.observeCategories(CategoryType.INCOME),
    ) { form, expense, income ->
        form.copy(categories = if (form.type == CategoryType.EXPENSE) expense else income)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), form.value)

    /** ViewModel hidup sepanjang Activity, jadi form dikosongkan setiap kali layar ditutup. */
    fun reset() {
        form.value = initialState()
    }

    fun selectType(type: CategoryType) = form.update {
        // Kategori beda per tipe, jadi pilihan lama tidak berlaku lagi.
        if (it.type == type) it else it.copy(type = type, categoryId = null, error = null)
    }

    fun selectCategory(id: Long) = form.update { it.copy(categoryId = id, error = null) }

    fun selectDate(date: LocalDate) = form.update { it.copy(date = date, error = null) }

    fun setRepeatMonthly(repeat: Boolean) = form.update { it.copy(repeatMonthly = repeat) }

    /** [amountDigits] berisi angka saja, tanpa pemisah ribuan. */
    fun save(amountDigits: String, note: String) {
        val current = form.value
        if (current.saving) return
        val amount = amountDigits.toLongOrNull()
        val error = validateTransaction(amount, current.categoryId, current.date, LocalDate.now(clock))
        if (error != null) {
            form.update { it.copy(error = error) }
            return
        }
        form.update { it.copy(saving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                repository.addToMainWallet(
                    type = if (current.type == CategoryType.EXPENSE) TransactionType.EXPENSE else TransactionType.INCOME,
                    amount = amount!!,
                    categoryId = current.categoryId!!,
                    note = note.trim().ifEmpty { null },
                    date = current.date,
                    repeatMonthly = current.repeatMonthly,
                )
                form.update { it.copy(saving = false, saved = true) }
            } catch (_: SQLiteException) {
                // Jangan crash; isian tetap ada agar pengguna bisa coba lagi (§7.3).
                form.update { it.copy(saving = false, saveFailed = true) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as ItungEnApp).container
                AddTransactionViewModel(container.transactionRepository, container.clock)
            }
        }
    }
}
