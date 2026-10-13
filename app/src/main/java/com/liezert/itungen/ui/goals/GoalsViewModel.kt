package com.liezert.itungen.ui.goals

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.model.Goal
import com.liezert.itungen.domain.repository.GoalRepository
import com.liezert.itungen.domain.repository.TransactionRepository
import com.liezert.itungen.domain.usecase.GoalError
import com.liezert.itungen.domain.usecase.InsufficientBalanceException
import com.liezert.itungen.domain.usecase.TransferError
import com.liezert.itungen.domain.usecase.validateGoal
import com.liezert.itungen.domain.usecase.validateTransfer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

/** Status form di sheet Buat Target / Tambah Dana. */
data class GoalFormState(
    val busy: Boolean = false,
    val goalError: GoalError? = null,
    val transferError: TransferError? = null,
    val failed: Boolean = false,
    /** Naik setiap aksi berhasil; layar menutup sheet saat nilainya berubah. */
    val doneCount: Int = 0,
)

data class GoalsUiState(
    val today: LocalDate,
    val loading: Boolean = true,
    val goals: List<Goal> = emptyList(),
    val mainBalance: Long = 0,
    val form: GoalFormState = GoalFormState(),
)

class GoalsViewModel(
    private val goals: GoalRepository,
    transactions: TransactionRepository,
    private val clock: Clock,
) : ViewModel() {
    private val form = MutableStateFlow(GoalFormState())

    val uiState: StateFlow<GoalsUiState> = combine(goals.observeGoals(), transactions.observeMainBalance(), form) { list, balance, f ->
        GoalsUiState(today = LocalDate.now(clock), loading = false, goals = list, mainBalance = balance, form = f)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState(today = LocalDate.now(clock)))

    /** Dipanggil saat sheet dibuka agar pesan error lama tidak terbawa. */
    fun clearErrors() = form.update { it.copy(goalError = null, transferError = null, failed = false) }

    fun createGoal(title: String, target: Long?, targetDate: LocalDate?, plannedMonthly: Long?) {
        val error = validateGoal(title, target, targetDate, LocalDate.now(clock))
        if (error != null) {
            form.update { it.copy(goalError = error) }
            return
        }
        perform { goals.createGoal(title.trim(), target!!, targetDate, plannedMonthly ?: 0) }
    }

    fun addFunds(goalId: Long, amount: Long?) {
        val error = validateTransfer(amount, uiState.value.mainBalance)
        if (error != null) {
            form.update { it.copy(transferError = error) }
            return
        }
        perform { goals.addFunds(goalId, amount!!) }
    }

    fun cancelGoal(goalId: Long) = perform { goals.cancelGoal(goalId) }

    private fun perform(action: suspend () -> Unit) {
        if (form.value.busy) return
        form.update { it.copy(busy = true, failed = false) }
        viewModelScope.launch {
            try {
                action()
                form.update { GoalFormState(doneCount = it.doneCount + 1) }
            } catch (_: InsufficientBalanceException) {
                form.update { it.copy(busy = false, transferError = TransferError.INSUFFICIENT_BALANCE) }
            } catch (_: SQLiteException) {
                // Pesan umum, jangan crash; transaksi database sudah dibatalkan (§7.3).
                form.update { it.copy(busy = false, failed = true) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as ItungEnApp).container
                GoalsViewModel(c.goalRepository, c.transactionRepository, c.clock)
            }
        }
    }
}
