package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.portfolio.manager.domain.model.FIRECalculation
import com.portfolio.manager.domain.model.FIRETargetCalculation
import com.portfolio.manager.domain.model.GroupFireSettings
import com.portfolio.manager.domain.repository.SyncRepository
import com.portfolio.manager.domain.service.PortfolioCache
import com.portfolio.manager.domain.util.CurrencyConverter
import com.portfolio.manager.util.AppConstants.KRW_TO_USD_RATE
import com.portfolio.manager.util.ErrorMessages
import com.portfolio.manager.util.PreferenceKeys
import com.portfolio.manager.util.boolean
import com.portfolio.manager.util.double

sealed interface FIRECalculatorUiState {
    data object Loading : FIRECalculatorUiState
    data class Success(
        val totalPortfolioValue: Double,
        val showInKrw: Boolean,
        val exchangeRate: Double,
        val annualReturn: Double,
        val annualInflation: Double,
        val targetMonthlySpending: Double,
        val fireCalculation: FIRECalculation,
        val fireTargetCalculation: FIRETargetCalculation
    ) : FIRECalculatorUiState
    data class Error(val message: String) : FIRECalculatorUiState
}

@HiltViewModel
class FIRECalculatorViewModel @Inject constructor(
    private val portfolioCache: PortfolioCache,
    private val syncRepository: SyncRepository,
    private val sharedPreferences: SharedPreferences,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        const val DEFAULT_ANNUAL_RETURN = 7.0
        const val DEFAULT_ANNUAL_INFLATION = 2.0
        const val DEFAULT_TARGET_MONTHLY_SPENDING = 3000.0
        const val ARG_GROUP = "group"
    }

    // Group mode (opened from the household screen) shares its settings via
    // Firebase; personal mode (from the dashboard) keeps them in local prefs.
    private val isGroup: Boolean = savedStateHandle.get<Boolean>(ARG_GROUP) ?: false

    private val householdCode: String?
        get() = sharedPreferences.getString(PreferenceKeys.HOUSEHOLD_CODE, null)

    private val _uiState = MutableStateFlow<FIRECalculatorUiState>(FIRECalculatorUiState.Loading)
    val uiState: StateFlow<FIRECalculatorUiState> = _uiState.asStateFlow()

    private var currentExchangeRate: Double = KRW_TO_USD_RATE

    // Personal settings persisted locally; also seed the group settings.
    private var localAnnualReturn by sharedPreferences.double(PreferenceKeys.FIRE_ANNUAL_RETURN, DEFAULT_ANNUAL_RETURN)
    private var localAnnualInflation by sharedPreferences.double(PreferenceKeys.FIRE_ANNUAL_INFLATION, DEFAULT_ANNUAL_INFLATION)
    private var localTargetMonthlySpending by sharedPreferences.double(PreferenceKeys.FIRE_TARGET_MONTHLY_SPENDING, DEFAULT_TARGET_MONTHLY_SPENDING)
    private var localTargetSpendingInKrw by sharedPreferences.boolean(PreferenceKeys.FIRE_TARGET_SPENDING_IN_KRW, false)

    // Effective settings used in calculations (diverge from local in group mode).
    private var annualReturn = localAnnualReturn
    private var annualInflation = localAnnualInflation
    // Target spending stored in the currency user entered it
    private var targetMonthlySpending = localTargetMonthlySpending
    private var targetSpendingInKrw = localTargetSpendingInKrw

    // Display currency is always a per-viewer local preference.
    private var showInKrw by sharedPreferences.boolean(PreferenceKeys.FIRE_SHOW_IN_KRW, false)

    init {
        loadData()
    }

    private fun loadData() {
        // Read from shared cache (populated by Dashboard/Household)
        if (!portfolioCache.hasData()) {
            _uiState.value = FIRECalculatorUiState.Error(ErrorMessages.NO_PORTFOLIO_DATA)
            return
        }

        if (isGroup) {
            viewModelScope.launch {
                loadGroupSettings()
                emitFromCache()
            }
        } else {
            emitFromCache()
        }
    }

    private fun emitFromCache() {
        currentExchangeRate = portfolioCache.exchangeRate

        val stocks = portfolioCache.stocks
        val cashItems = portfolioCache.cashItems

        // Calculate stocks value in USD
        val stocksValueUsd = stocks.sumOf { it.totalValueInUsd(currentExchangeRate) }

        // Calculate cash value in USD
        val cashValueUsd = cashItems.sumOf { it.valueInUsd(currentExchangeRate) }

        // Total portfolio value = stocks + cash
        val totalValueUsd = stocksValueUsd + cashValueUsd

        updateState(totalValueUsd)
    }

    /**
     * Loads the household's shared FIRE settings from Firebase, seeding them
     * from the local values the first time (when none exist yet).
     */
    private suspend fun loadGroupSettings() {
        val code = householdCode ?: return
        val remote = syncRepository.fetchGroupFireSettings(code).getOrNull()
        if (remote != null) {
            annualReturn = remote.annualReturn
            annualInflation = remote.annualInflation
            targetMonthlySpending = remote.targetMonthlySpending
            targetSpendingInKrw = remote.targetSpendingInKrw
        } else {
            syncRepository.saveGroupFireSettings(code, currentGroupSettings())
        }
    }

    private fun currentGroupSettings() = GroupFireSettings(
        annualReturn = annualReturn,
        annualInflation = annualInflation,
        targetMonthlySpending = targetMonthlySpending,
        targetSpendingInKrw = targetSpendingInKrw
    )

    /** Persists the current settings to the active store (Firebase or local prefs). */
    private fun persistSettings() {
        if (isGroup) {
            val code = householdCode ?: return
            viewModelScope.launch { syncRepository.saveGroupFireSettings(code, currentGroupSettings()) }
        } else {
            localAnnualReturn = annualReturn
            localAnnualInflation = annualInflation
            localTargetMonthlySpending = targetMonthlySpending
            localTargetSpendingInKrw = targetSpendingInKrw
        }
    }

    private fun updateState(totalPortfolioValueUsd: Double) {
        val displayValue = CurrencyConverter.toDisplayCurrency(totalPortfolioValueUsd, showInKrw, currentExchangeRate)
        val displaySpending = convertSpendingToDisplayCurrency()

        val fireCalculation = calculateFIRE(displayValue, annualReturn, annualInflation)
        val fireTargetCalculation = calculateFIRETarget(
            displaySpending, displayValue, annualReturn, annualInflation
        )

        _uiState.value = FIRECalculatorUiState.Success(
            totalPortfolioValue = displayValue,
            showInKrw = showInKrw,
            exchangeRate = currentExchangeRate,
            annualReturn = annualReturn,
            annualInflation = annualInflation,
            targetMonthlySpending = displaySpending,
            fireCalculation = fireCalculation,
            fireTargetCalculation = fireTargetCalculation
        )
    }

    /**
     * Converts stored target spending to current display currency.
     */
    private fun convertSpendingToDisplayCurrency(): Double {
        return when {
            targetSpendingInKrw == showInKrw -> targetMonthlySpending // Same currency, no conversion
            targetSpendingInKrw -> targetMonthlySpending / currentExchangeRate // KRW to USD
            else -> targetMonthlySpending * currentExchangeRate // USD to KRW
        }
    }

    fun updateAnnualReturn(value: Double) {
        annualReturn = value
        persistSettings()
        recalculate()
    }

    fun updateAnnualInflation(value: Double) {
        annualInflation = value
        persistSettings()
        recalculate()
    }

    fun updateTargetMonthlySpending(value: Double) {
        // Store in the currency user entered it
        targetMonthlySpending = value
        targetSpendingInKrw = showInKrw
        persistSettings()
        recalculate()
    }

    fun toggleCurrency() {
        showInKrw = !showInKrw
        recalculate()
    }

    fun refresh() {
        loadData()
    }

    private fun recalculate() {
        val currentState = _uiState.value
        if (currentState is FIRECalculatorUiState.Success) {
            val baseValueUsd = CurrencyConverter.fromDisplayCurrency(currentState.totalPortfolioValue, currentState.showInKrw, currentExchangeRate)
            val displayValue = CurrencyConverter.toDisplayCurrency(baseValueUsd, showInKrw, currentExchangeRate)
            val displaySpending = convertSpendingToDisplayCurrency()

            val fireCalculation = calculateFIRE(displayValue, annualReturn, annualInflation)
            val fireTargetCalculation = calculateFIRETarget(
                displaySpending, displayValue, annualReturn, annualInflation
            )

            _uiState.value = currentState.copy(
                totalPortfolioValue = displayValue,
                showInKrw = showInKrw,
                annualReturn = annualReturn,
                annualInflation = annualInflation,
                targetMonthlySpending = displaySpending,
                fireCalculation = fireCalculation,
                fireTargetCalculation = fireTargetCalculation
            )
        }
    }

    private fun calculateFIRE(
        portfolioValue: Double,
        annualReturn: Double,
        annualInflation: Double
    ): FIRECalculation {
        // Real return = nominal return - inflation (simplified Fisher equation)
        val realReturn = annualReturn - annualInflation

        // Sustainable spending is based on real return to maintain purchasing power
        val sustainableAnnualSpending = if (realReturn > 0) {
            portfolioValue * (realReturn / 100.0)
        } else {
            0.0
        }
        val sustainableMonthlySpending = sustainableAnnualSpending / 12.0

        return FIRECalculation(
            totalPortfolioValue = portfolioValue,
            annualReturn = annualReturn,
            annualInflation = annualInflation,
            realReturn = realReturn,
            sustainableMonthlySpending = sustainableMonthlySpending,
            sustainableAnnualSpending = sustainableAnnualSpending
        )
    }

    private fun calculateFIRETarget(
        targetMonthlySpending: Double,
        currentPortfolio: Double,
        annualReturn: Double,
        annualInflation: Double
    ): FIRETargetCalculation {
        val realReturn = annualReturn - annualInflation

        // Required portfolio = target annual spending / real return rate
        // Use minimum 0.01% to avoid extremely large numbers from near-zero returns
        val targetAnnualSpending = targetMonthlySpending * 12
        val requiredPortfolio = if (realReturn >= 0.01) {
            targetAnnualSpending / (realReturn / 100.0)
        } else {
            Double.POSITIVE_INFINITY
        }

        val progressPercent = if (requiredPortfolio > 0 && requiredPortfolio.isFinite()) {
            (currentPortfolio / requiredPortfolio * 100).coerceAtMost(100.0)
        } else {
            0.0
        }

        val remainingAmount = (requiredPortfolio - currentPortfolio).coerceAtLeast(0.0)

        return FIRETargetCalculation(
            targetMonthlySpending = targetMonthlySpending,
            requiredPortfolio = requiredPortfolio,
            currentPortfolio = currentPortfolio,
            progressPercent = progressPercent,
            remainingAmount = remainingAmount
        )
    }
}
