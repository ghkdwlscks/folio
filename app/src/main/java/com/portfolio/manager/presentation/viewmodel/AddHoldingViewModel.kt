package com.portfolio.manager.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.repository.HoldingsRepository
import kotlinx.coroutines.launch

class AddHoldingViewModel(
    private val repository: HoldingsRepository,
    private val accountId: Long
) : ViewModel() {

    val symbol = mutableStateOf("")
    val name = mutableStateOf("")
    val quantity = mutableStateOf("")
    val averagePrice = mutableStateOf("")
    val currency = mutableStateOf("USD")
    val errorMessage = mutableStateOf<String?>(null)

    suspend fun saveHolding(): Boolean {
        errorMessage.value = null
        val symbolValue = symbol.value.trim().uppercase()
        val nameValue = name.value.trim()
        val quantityValue = quantity.value.toIntOrNull()
        val priceValue = averagePrice.value.toDoubleOrNull()

        if (symbolValue.isEmpty() || quantityValue == null || priceValue == null) {
            return false
        }

        // Check for duplicate symbol in the same account
        val existingHolding = repository.getHoldingByAccountAndSymbol(accountId, symbolValue)
        if (existingHolding != null) {
            errorMessage.value = "This stock already exists in the account"
            return false
        }

        val holding = HoldingEntity(
            accountId = accountId,
            symbol = symbolValue,
            name = nameValue.ifEmpty { symbolValue },
            quantity = quantityValue,
            averagePrice = priceValue,
            currency = currency.value
        )

        repository.addHolding(holding)
        return true
    }
}
