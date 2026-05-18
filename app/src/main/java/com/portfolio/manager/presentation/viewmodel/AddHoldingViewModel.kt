package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.presentation.util.InputUtils
import com.portfolio.manager.presentation.viewmodel.base.FormUiState
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import com.portfolio.manager.util.AppConstants.MAX_PRICE
import com.portfolio.manager.util.AppConstants.MAX_QUANTITY
import com.portfolio.manager.util.AppConstants.MIN_PRICE
import com.portfolio.manager.util.AppConstants.MIN_QUANTITY
import com.portfolio.manager.util.ErrorMessages

data class AddHoldingUiState(
    override val isInitialLoading: Boolean = true,
    val symbol: String = "",
    val quantity: String = "",
    val averagePrice: String = "",
    override val currency: Currency = Currency.USD,
    override val errorMessage: String? = null,
    override val selectedAccountId: Long = ALL_ACCOUNTS_ID,
    override val accounts: List<AccountEntity> = emptyList(),
    override val needsAccountSelection: Boolean = false,
    override val isEditMode: Boolean = false,
    override val isSaving: Boolean = false
) : FormUiState

@HiltViewModel
class AddHoldingViewModel @Inject constructor(
    private val repository: HoldingsRepository,
    private val accountRepository: AccountRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val holdingId: Long? = savedStateHandle.get<Long>("holdingId")
    private val initialAccountId: Long = savedStateHandle.get<Long>("accountId") ?: ALL_ACCOUNTS_ID
    private var existingTargetPercentage: Int? = null

    private val _uiState = MutableStateFlow(AddHoldingUiState(
        selectedAccountId = initialAccountId,
        isEditMode = holdingId != null
    ))
    val uiState: StateFlow<AddHoldingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Load accounts
            val accountList = accountRepository.getAllAccounts().first()
            _uiState.update { it.copy(accounts = accountList) }

            // Set initial account selection
            if (initialAccountId == ALL_ACCOUNTS_ID && accountList.isNotEmpty() && holdingId == null) {
                _uiState.update { it.copy(
                    selectedAccountId = accountList.first().id,
                    needsAccountSelection = true
                )}
            }

            // Load existing holding for edit mode
            if (holdingId != null) {
                repository.getHoldingById(holdingId)?.let { holding ->
                    existingTargetPercentage = holding.targetPercentage
                    val holdingCurrency = Currency.fromCode(holding.currency)
                    val priceStr = InputUtils.formatValueForCurrency(holding.averagePrice, holdingCurrency)
                    _uiState.update { it.copy(
                        symbol = holding.symbol,
                        quantity = holding.quantity.toString(),
                        averagePrice = priceStr,
                        currency = holdingCurrency,
                        selectedAccountId = holding.accountId
                    )}
                }
            }

            _uiState.update { it.copy(isInitialLoading = false) }
        }
    }

    fun updateSymbol(value: String) {
        if (!_uiState.value.isEditMode) {
            _uiState.update { it.copy(symbol = value.uppercase(), errorMessage = null) }
        }
    }

    fun updateQuantity(value: String) {
        _uiState.update { it.copy(quantity = InputUtils.filterDigitsOnly(value)) }
    }

    fun updateAveragePrice(value: String) {
        _uiState.update { it.copy(averagePrice = InputUtils.filterNumeric(value)) }
    }

    fun updateCurrency(value: Currency) {
        _uiState.update { it.copy(currency = value) }
    }

    fun selectAccount(accountId: Long) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun onSymbolFocusLost() {
        val symbol = _uiState.value.symbol.trim()
        if (isKoreanShortCode(symbol)) {
            _uiState.update { it.copy(currency = Currency.KRW) }
        }
    }

    private fun normalizeSymbol(symbol: String): String {
        val trimmed = symbol.trim().uppercase()
        return if (isKoreanShortCode(trimmed)) "$trimmed.KS" else trimmed
    }

    // KRX short codes are 6 alphanumeric characters starting with a digit.
    // Since 2024 KRX has mixed letters into later positions (e.g. 0060H0 for an ETF).
    private fun isKoreanShortCode(symbol: String): Boolean =
        symbol.length == 6 && symbol[0].isDigit() && symbol.all { it.isLetterOrDigit() }

    suspend fun saveHolding(): Boolean {
        if (_uiState.value.isSaving) return false
        _uiState.update { it.copy(errorMessage = null, isSaving = true) }
        try {
            val state = _uiState.value
            val symbolValue = normalizeSymbol(state.symbol)
            val quantityValue = state.quantity.toIntOrNull()
            val priceValue = state.averagePrice.toDoubleOrNull()
            val targetAccountId = state.selectedAccountId

            if (symbolValue.isEmpty() || quantityValue == null || priceValue == null) {
                return false
            }

            // Validate quantity bounds
            if (quantityValue < MIN_QUANTITY || quantityValue > MAX_QUANTITY) {
                _uiState.update { it.copy(errorMessage = ErrorMessages.invalidQuantity(MIN_QUANTITY, MAX_QUANTITY)) }
                return false
            }

            // Validate price bounds
            if (priceValue < MIN_PRICE || priceValue > MAX_PRICE) {
                _uiState.update { it.copy(errorMessage = ErrorMessages.invalidPrice(MIN_PRICE, MAX_PRICE)) }
                return false
            }

            if (targetAccountId == ALL_ACCOUNTS_ID) {
                _uiState.update { it.copy(errorMessage = ErrorMessages.SELECT_ACCOUNT) }
                return false
            }

            // Check for duplicate symbol (exclude current holding in edit mode)
            if (!validateSymbolUniqueness(targetAccountId, symbolValue, if (state.isEditMode) holdingId else null)) {
                return false
            }

            val holding = HoldingEntity(
                id = if (state.isEditMode && holdingId != null) holdingId else 0,
                accountId = targetAccountId,
                symbol = symbolValue,
                name = symbolValue,
                quantity = quantityValue,
                averagePrice = priceValue,
                currency = state.currency.code,
                targetPercentage = if (state.isEditMode) existingTargetPercentage else null
            )

            if (state.isEditMode) {
                repository.updateHolding(holding)
            } else {
                repository.addHolding(holding)
            }
            return true
        } finally {
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private suspend fun validateSymbolUniqueness(
        accountId: Long,
        symbol: String,
        excludeHoldingId: Long?
    ): Boolean {
        val existingHolding = repository.getHoldingByAccountAndSymbol(accountId, symbol)
        if (existingHolding != null && existingHolding.id != excludeHoldingId) {
            _uiState.update { it.copy(errorMessage = ErrorMessages.DUPLICATE_SYMBOL) }
            return false
        }
        return true
    }
}
