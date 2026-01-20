package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.domain.repository.AccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface AccountsUiState {
    data object Loading : AccountsUiState
    data class Success(val accounts: List<AccountEntity>) : AccountsUiState
}

class AccountsViewModel(
    private val repository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccountsUiState>(AccountsUiState.Loading)
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllAccounts().collectLatest { accounts ->
                _uiState.value = AccountsUiState.Success(accounts)
            }
        }
    }

    fun addAccount(name: String) {
        viewModelScope.launch {
            val maxOrder = repository.getMaxOrderIndex()
            val account = AccountEntity(name = name, orderIndex = maxOrder + 1)
            repository.addAccount(account)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
        }
    }

    fun renameAccount(accountId: Long, newName: String) {
        viewModelScope.launch {
            val account = repository.getAccountById(accountId)
            if (account != null) {
                repository.updateAccount(account.copy(name = newName))
            }
        }
    }

    fun reorderAccounts(reorderedAccounts: List<AccountEntity>) {
        viewModelScope.launch {
            val updatedAccounts = reorderedAccounts.mapIndexed { index, account ->
                account.copy(orderIndex = index)
            }
            repository.updateAccounts(updatedAccounts)
        }
    }
}
