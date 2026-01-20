package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AccountsUiState {
    data object Loading : AccountsUiState
    data class Success(
        val accounts: List<AccountEntity>,
        val errorMessage: String? = null
    ) : AccountsUiState
    data class Error(val message: String) : AccountsUiState
}

@HiltViewModel
class AccountsViewModel @Inject constructor(
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
            try {
                val maxOrder = repository.getMaxOrderIndex()
                val account = AccountEntity(name = name, orderIndex = maxOrder + 1)
                repository.addAccount(account)
            } catch (e: Exception) {
                showError("Failed to add account: ${e.message}")
            }
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(accountId)
            } catch (e: Exception) {
                showError("Failed to delete account: ${e.message}")
            }
        }
    }

    fun renameAccount(accountId: Long, newName: String) {
        viewModelScope.launch {
            try {
                val account = repository.getAccountById(accountId)
                if (account != null) {
                    repository.updateAccount(account.copy(name = newName))
                }
            } catch (e: Exception) {
                showError("Failed to rename account: ${e.message}")
            }
        }
    }

    fun reorderAccounts(reorderedAccounts: List<AccountEntity>) {
        viewModelScope.launch {
            try {
                val updatedAccounts = reorderedAccounts.mapIndexed { index, account ->
                    account.copy(orderIndex = index)
                }
                repository.updateAccounts(updatedAccounts)
            } catch (e: Exception) {
                showError("Failed to reorder accounts: ${e.message}")
            }
        }
    }

    fun clearError() {
        val current = _uiState.value
        if (current is AccountsUiState.Success) {
            _uiState.value = current.copy(errorMessage = null)
        }
    }

    private fun showError(message: String) {
        val current = _uiState.value
        if (current is AccountsUiState.Success) {
            _uiState.value = current.copy(errorMessage = message)
        } else {
            _uiState.value = AccountsUiState.Error(message)
        }
    }
}
