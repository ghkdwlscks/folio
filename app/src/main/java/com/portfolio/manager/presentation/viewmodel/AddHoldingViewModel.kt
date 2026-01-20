package com.portfolio.manager.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.repository.HoldingsRepository
import kotlinx.coroutines.launch

class AddHoldingViewModel(
    private val repository: HoldingsRepository
) : ViewModel() {

    val symbol = mutableStateOf("")
    val name = mutableStateOf("")
    val quantity = mutableStateOf("")
    val averagePrice = mutableStateOf("")
    val currency = mutableStateOf("USD")

    suspend fun saveHolding(): Boolean {
        val symbolValue = symbol.value.trim()
        val nameValue = name.value.trim()
        val quantityValue = quantity.value.toIntOrNull()
        val priceValue = averagePrice.value.toDoubleOrNull()

        if (symbolValue.isEmpty() || quantityValue == null || priceValue == null) {
            return false
        }

        val holding = HoldingEntity(
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
