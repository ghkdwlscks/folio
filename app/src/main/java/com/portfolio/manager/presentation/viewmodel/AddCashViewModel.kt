package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.CashRepository
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddCashUiState(
    val name: String = "",
    val value: String = "",
    val yieldRate: String = "",
    val currency: String = "KRW",
    val errorMessage: String? = null,
    val selectedAccountId: Long = ALL_ACCOUNTS_ID,
    val accounts: List<AccountEntity> = emptyList(),
    val needsAccountSelection: Boolean = false,
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class AddCashViewModel @Inject constructor(
    private val cashRepository: CashRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val cashItemId: Long? = savedStateHandle.get<Long>("cashItemId")
    private val initialAccountId: Long = savedStateHandle.get<Long>("accountId") ?: ALL_ACCOUNTS_ID

    private val _uiState = MutableStateFlow(AddCashUiState(
        selectedAccountId = initialAccountId,
        isEditMode = cashItemId != null
    ))
    val uiState: StateFlow<AddCashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Load accounts
            val accountList = accountRepository.getAllAccounts().first()
            _uiState.update { it.copy(accounts = accountList) }

            // Set initial account selection
            if (initialAccountId == ALL_ACCOUNTS_ID && accountList.isNotEmpty() && cashItemId == null) {
                _uiState.update { it.copy(
                    selectedAccountId = accountList.first().id,
                    needsAccountSelection = true
                )}
            }

            // Load existing cash item for edit mode
            if (cashItemId != null) {
                cashRepository.getCashItemById(cashItemId)?.let { cashItem ->
                    // Format value as integer for KRW, with decimals for USD
                    val valueStr = if (cashItem.currency == "KRW") {
                        cashItem.originalValue.toLong().toString()
                    } else {
                        cashItem.originalValue.toString()
                    }
                    _uiState.update { it.copy(
                        name = cashItem.name,
                        value = valueStr,
                        yieldRate = cashItem.annualYieldRate.toString(),
                        currency = cashItem.currency,
                        selectedAccountId = cashItem.accountId
                    )}
                }
            }
        }
    }

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun updateValue(value: String) {
        _uiState.update { it.copy(value = value.filter { c -> c.isDigit() || c == '.' }) }
    }

    fun updateYieldRate(value: String) {
        _uiState.update { it.copy(yieldRate = value.filter { c -> c.isDigit() || c == '.' }) }
    }

    fun updateCurrency(value: String) {
        _uiState.update { it.copy(currency = value) }
    }

    fun selectAccount(accountId: Long) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    suspend fun saveCashItem(): Boolean {
        if (_uiState.value.isSaving) return false
        _uiState.update { it.copy(errorMessage = null, isSaving = true) }
        try {
            val state = _uiState.value
            val nameValue = state.name.trim()
            val valueAmount = state.value.toDoubleOrNull()
            val yieldRateValue = state.yieldRate.toDoubleOrNull()
            val targetAccountId = state.selectedAccountId

            if (nameValue.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Please enter a name") }
                return false
            }

            if (valueAmount == null || valueAmount <= 0) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid value") }
                return false
            }

            if (yieldRateValue == null || yieldRateValue < 0) {
                _uiState.update { it.copy(errorMessage = "Please enter a valid yield rate") }
                return false
            }

            if (targetAccountId == ALL_ACCOUNTS_ID) {
                _uiState.update { it.copy(errorMessage = "Please select an account") }
                return false
            }

            if (state.isEditMode && cashItemId != null) {
                cashRepository.updateCashItem(cashItemId, nameValue, valueAmount, yieldRateValue, state.currency)
            } else {
                cashRepository.addCashItem(targetAccountId, nameValue, valueAmount, yieldRateValue, state.currency)
            }
            return true
        } finally {
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
