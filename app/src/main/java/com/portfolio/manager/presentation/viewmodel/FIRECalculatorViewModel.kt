package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.portfolio.manager.domain.model.FIRECalculation
import com.portfolio.manager.domain.model.FIRETargetCalculation
import com.portfolio.manager.domain.service.PortfolioCache
import com.portfolio.manager.domain.util.CurrencyConverter
import com.portfolio.manager.util.AppConstants.KRW_TO_USD_RATE
import com.portfolio.manager.util.PreferenceKeys
import com.portfolio.manager.util.boolean
import com.portfolio.manager.util.double
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

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
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<FIRECalculatorUiState>(FIRECalculatorUiState.Loading)
    val uiState: StateFlow<FIRECalculatorUiState> = _uiState.asStateFlow()

    private var currentExchangeRate: Double = KRW_TO_USD_RATE

    private var annualReturn by sharedPreferences.double(PreferenceKeys.FIRE_ANNUAL_RETURN, DEFAULT_ANNUAL_RETURN)

    private var annualInflation by sharedPreferences.double(PreferenceKeys.FIRE_ANNUAL_INFLATION, DEFAULT_ANNUAL_INFLATION)

    // Target spending stored in the currency user entered it
    private var targetMonthlySpending by sharedPreferences.double(PreferenceKeys.FIRE_TARGET_MONTHLY_SPENDING, DEFAULT_TARGET_MONTHLY_SPENDING)
    private var targetSpendingInKrw by sharedPreferences.boolean(PreferenceKeys.FIRE_TARGET_SPENDING_IN_KRW, false)

    private var showInKrw by sharedPreferences.boolean(PreferenceKeys.FIRE_SHOW_IN_KRW, false)

    companion object {
        const val DEFAULT_ANNUAL_RETURN = 7.0
        const val DEFAULT_ANNUAL_INFLATION = 2.0
        const val DEFAULT_TARGET_MONTHLY_SPENDING = 3000.0
    }

    init {
        loadData()
    }

    private fun loadData() {
        // Read from shared cache (populated by DashboardViewModel)
        if (!portfolioCache.hasData()) {
            _uiState.value = FIRECalculatorUiState.Error("No portfolio data. Please refresh the dashboard first.")
            return
        }

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
        recalculate()
    }

    fun updateAnnualInflation(value: Double) {
        annualInflation = value
        recalculate()
    }

    fun updateTargetMonthlySpending(value: Double) {
        // Store in the currency user entered it
        targetMonthlySpending = value
        targetSpendingInKrw = showInKrw
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
