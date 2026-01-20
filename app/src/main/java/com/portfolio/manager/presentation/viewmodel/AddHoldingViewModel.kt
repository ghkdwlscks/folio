package com.portfolio.manager.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.repository.HoldingsRepository
import kotlinx.coroutines.launch

class AddHoldingViewModel(
    private val repository: HoldingsRepository,
    private val initialAccountId: Long,
    val accounts: List<AccountEntity> = emptyList(),
    private val holdingId: Long? = null
) : ViewModel() {

    val symbol = mutableStateOf("")
    val quantity = mutableStateOf("")
    val averagePrice = mutableStateOf("")
    val currency = mutableStateOf("USD")
    val errorMessage = mutableStateOf<String?>(null)
    val selectedAccountId = mutableStateOf(
        if (initialAccountId == ALL_ACCOUNTS_ID && accounts.isNotEmpty()) {
            accounts.first().id
        } else {
            initialAccountId
        }
    )

    val isEditMode: Boolean = holdingId != null
    val needsAccountSelection: Boolean = initialAccountId == ALL_ACCOUNTS_ID && accounts.isNotEmpty() && !isEditMode

    init {
        if (holdingId != null) {
            viewModelScope.launch {
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
