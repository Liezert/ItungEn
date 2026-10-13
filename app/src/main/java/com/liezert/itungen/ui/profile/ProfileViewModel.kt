package com.liezert.itungen.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.liezert.itungen.ItungEnApp
import com.liezert.itungen.domain.model.GoalStatus
import com.liezert.itungen.domain.repository.GoalRepository
import com.liezert.itungen.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ProfileUiState(
    val balance: Long = 0,
    /** Tanggal pertama app dipakai; null selama memuat. */
    val joined: LocalDate? = null,
    val activeGoals: Int = 0,
)

class ProfileViewModel(repository: TransactionRepository, goals: GoalRepository) : ViewModel() {
    val uiState: StateFlow<ProfileUiState> = combine(
        repository.observeMainBalance(),
        flow { emit(repository.firstUseDate()) },
        goals.observeGoals(),
    ) { balance, joined, goalList ->
        ProfileUiState(balance = balance, joined = joined, activeGoals = goalList.count { it.status == GoalStatus.ACTIVE })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as ItungEnApp).container
                ProfileViewModel(c.transactionRepository, c.goalRepository)
            }
        }
    }
}
