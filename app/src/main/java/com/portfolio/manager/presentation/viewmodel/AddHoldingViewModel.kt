package com.portfolio.manager.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddHoldingViewModel @Inject constructor(
    private val repository: HoldingsRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val holdingId: Long? = savedStateHandle.get<Long>("holdingId")
    private val initialAccountId: Long = savedStateHandle.get<Long>("accountId") ?: ALL_ACCOUNTS_ID

    val symbol = mutableStateOf("")
    val quantity = mutableStateOf("")
    val averagePrice = mutableStateOf("")
    val currency = mutableStateOf("USD")
    val errorMessage = mutableStateOf<String?>(null)
    val selectedAccountId = mutableStateOf(initialAccountId)
    val accounts = mutableStateOf<List<AccountEntity>>(emptyList())

    val isEditMode: Boolean = holdingId != null
    val needsAccountSelection = mutableStateOf(false)

    init {
        viewModelScope.launch {
            // Load accounts
            val accountList = accountRepository.getAllAccounts().first()
            accounts.value = accountList

            // Set initial account selection
            if (initialAccountId == ALL_ACCOUNTS_ID && accountList.isNotEmpty() && !isEditMode) {
                selectedAccountId.value = accountList.first().id
                needsAccountSelection.value = true
            }

            // Load existing holding for edit mode
            if (holdingId != null) {
                repository.getHoldingById(holdingId)?.let { holding ->
                    symbol.value = holding.symbol
                    quantity.value = holding.quantity.toString()
                    averagePrice.value = holding.averagePrice.toString()
                    currency.value = holding.currency
                    selectedAccountId.value = holding.accountId
                }
            }
        }
    }

    fun selectAccount(accountId: Long) {
        selectedAccountId.value = accountId
    }

    suspend fun saveHolding(): Boolean {
        errorMessage.value = null
        val symbolValue = symbol.value.trim().uppercase()
        val quantityValue = quantity.value.toIntOrNull()
        val priceValue = averagePrice.value.toDoubleOrNull()
        val targetAccountId = selectedAccountId.value

        if (symbolValue.isEmpty() || quantityValue == null || priceValue == null) {
            return false
        }

        if (targetAccountId == ALL_ACCOUNTS_ID) {
            errorMessage.value = "Please select an account"
            return false
        }

        if (isEditMode) {
            // Update existing holding
            val holding = HoldingEntity(
                id = holdingId!!,
                accountId = targetAccountId,
                symbol = symbolValue,
                name = symbolValue,
                quantity = quantityValue,
                averagePrice = priceValue,
                currency = currency.value
            )
            repository.updateHolding(holding)
        } else {
            // Check for duplicate symbol in the same account
            val existingHolding = repository.getHoldingByAccountAndSymbol(targetAccountId, symbolValue)
            if (existingHolding != null) {
                errorMessage.value = "This stock already exists in the account"
                return false
            }

            val holding = HoldingEntity(
                accountId = targetAccountId,
                symbol = symbolValue,
                name = symbolValue,
                quantity = quantityValue,
                averagePrice = priceValue,
                currency = currency.value
            )
            repository.addHolding(holding)
        }
        return true
    }
}
