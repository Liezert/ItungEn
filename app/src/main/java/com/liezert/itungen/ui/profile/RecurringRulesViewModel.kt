package com.liezert.itungen.ui.profile

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.model.RecurringRule
import com.liezert.itungen.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RecurringRulesUiState(
    val loading: Boolean = true,
    val rules: List<RecurringRule> = emptyList(),
    val actionFailed: Boolean = false,
)

class RecurringRulesViewModel(private val repository: TransactionRepository) : ViewModel() {
    private val actionFailed = MutableStateFlow(false)

    val uiState: StateFlow<RecurringRulesUiState> =
        combine(repository.observeRecurringRules(), actionFailed) { rules, failed ->
            RecurringRulesUiState(loading = false, rules = rules, actionFailed = failed)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringRulesUiState())

    fun setActive(id: Long, active: Boolean) = perform { repository.setRecurringRuleActive(id, active) }

    fun delete(id: Long) = perform { repository.deleteRecurringRule(id) }

    private fun perform(action: suspend () -> Unit) {
        actionFailed.value = false
        viewModelScope.launch {
            try {
                action()
            } catch (_: SQLiteException) {
                actionFailed.value = true // tampilkan pesan, jangan crash (§7.3)
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { RecurringRulesViewModel((this[APPLICATION_KEY] as ItungEnApp).container.transactionRepository) }
        }
    }
}
