package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.domain.model.FIRECalculation
import com.portfolio.manager.domain.model.FIRETargetCalculation
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.util.AppConstants.KRW_TO_USD_RATE
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private val stockRepository: StockRepository,
    private val holdingsRepository: HoldingsRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<FIRECalculatorUiState>(FIRECalculatorUiState.Loading)
    val uiState: StateFlow<FIRECalculatorUiState> = _uiState.asStateFlow()

    private var currentExchangeRate: Double = KRW_TO_USD_RATE

    private var annualReturn: Double
        get() = sharedPreferences.getFloat(PREF_ANNUAL_RETURN, DEFAULT_ANNUAL_RETURN.toFloat()).toDouble()
        set(value) = sharedPreferences.edit().putFloat(PREF_ANNUAL_RETURN, value.toFloat()).apply()

    private var annualInflation: Double
        get() = sharedPreferences.getFloat(PREF_ANNUAL_INFLATION, DEFAULT_ANNUAL_INFLATION.toFloat()).toDouble()
        set(value) = sharedPreferences.edit().putFloat(PREF_ANNUAL_INFLATION, value.toFloat()).apply()

    private var targetMonthlySpending: Double
        get() = sharedPreferences.getFloat(PREF_TARGET_MONTHLY_SPENDING, DEFAULT_TARGET_MONTHLY_SPENDING.toFloat()).toDouble()
        set(value) = sharedPreferences.edit().putFloat(PREF_TARGET_MONTHLY_SPENDING, value.toFloat()).apply()

    private var showInKrw: Boolean
        get() = sharedPreferences.getBoolean(PREF_FIRE_SHOW_IN_KRW, false)
        set(value) = sharedPreferences.edit().putBoolean(PREF_FIRE_SHOW_IN_KRW, value).apply()

    companion object {
        private const val PREF_ANNUAL_RETURN = "fire_annual_return"
        private const val PREF_ANNUAL_INFLATION = "fire_annual_inflation"
        private const val PREF_TARGET_MONTHLY_SPENDING = "fire_target_monthly_spending"
        private const val PREF_FIRE_SHOW_IN_KRW = "fire_show_in_krw"

        const val DEFAULT_ANNUAL_RETURN = 7.0
        const val DEFAULT_ANNUAL_INFLATION = 2.0
        const val DEFAULT_TARGET_MONTHLY_SPENDING = 3000.0
    }

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                _uiState.value = FIRECalculatorUiState.Loading

                // Fetch exchange rate
                currentExchangeRate = stockRepository.getExchangeRate("USD", "KRW")
                    .getOrDefault(KRW_TO_USD_RATE)

                // Get all holdings and calculate total portfolio value
                val holdings = holdingsRepository.getAllHoldings().first()

                if (holdings.isEmpty()) {
                    updateState(0.0)
                    return@launch
                }

                // Fetch current prices for all holdings
                val symbols = holdings.map { it.symbol }.distinct()
                val quotesResult = stockRepository.getQuotes(symbols)

                if (quotesResult.isFailure) {
                    _uiState.value = FIRECalculatorUiState.Error("Failed to fetch stock prices")
                    return@launch
                }

                val quotes = quotesResult.getOrThrow()
                val pricesBySymbol = quotes.associateBy { it.symbol }

                // Calculate total portfolio value in USD
                val totalValueUsd = holdings.sumOf { holding ->
                    val quote = pricesBySymbol[holding.symbol]
                    val currentPrice = quote?.regularMarketPrice ?: holding.averagePrice
                    val value = holding.quantity * currentPrice
                    if (holding.currency == "KRW") value / currentExchangeRate else value
                }

                updateState(totalValueUsd)
            } catch (e: Exception) {
                _uiState.value = FIRECalculatorUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private fun updateState(totalPortfolioValueUsd: Double) {
        val displayValue = if (showInKrw) {
            totalPortfolioValueUsd * currentExchangeRate
        } else {
            totalPortfolioValueUsd
        }

        val fireCalculation = calculateFIRE(displayValue, annualReturn, annualInflation)
        val fireTargetCalculation = calculateFIRETarget(
            targetMonthlySpending, displayValue, annualReturn, annualInflation
        )

        _uiState.value = FIRECalculatorUiState.Success(
            totalPortfolioValue = displayValue,
            showInKrw = showInKrw,
            exchangeRate = currentExchangeRate,
            annualReturn = annualReturn,
            annualInflation = annualInflation,
            targetMonthlySpending = targetMonthlySpending,
            fireCalculation = fireCalculation,
            fireTargetCalculation = fireTargetCalculation
        )
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
        targetMonthlySpending = value
        recalculate()
    }

    fun toggleCurrency() {
        val wasInKrw = showInKrw
        showInKrw = !showInKrw

        // Convert target monthly spending to new currency
        targetMonthlySpending = if (wasInKrw) {
            // Was KRW, now USD: divide by exchange rate
            targetMonthlySpending / currentExchangeRate
        } else {
            // Was USD, now KRW: multiply by exchange rate
            targetMonthlySpending * currentExchangeRate
        }

        recalculate()
    }

    fun refresh() {
        loadData()
    }

    private fun recalculate() {
        val currentState = _uiState.value
        if (currentState is FIRECalculatorUiState.Success) {
            val baseValueUsd = if (currentState.showInKrw) {
                currentState.totalPortfolioValue / currentExchangeRate
            } else {
                currentState.totalPortfolioValue
            }

            val displayValue = if (showInKrw) {
                baseValueUsd * currentExchangeRate
            } else {
                baseValueUsd
            }

            val fireCalculation = calculateFIRE(displayValue, annualReturn, annualInflation)
            val fireTargetCalculation = calculateFIRETarget(
                targetMonthlySpending, displayValue, annualReturn, annualInflation
            )

            _uiState.value = currentState.copy(
                totalPortfolioValue = displayValue,
                showInKrw = showInKrw,
                annualReturn = annualReturn,
                annualInflation = annualInflation,
                targetMonthlySpending = targetMonthlySpending,
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
        val targetAnnualSpending = targetMonthlySpending * 12
        val requiredPortfolio = if (realReturn > 0) {
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
