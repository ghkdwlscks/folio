package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val stocks: List<Stock>) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(
    private val stockRepository: StockRepository,
    private val holdingsRepository: HoldingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeHoldings()
    }

    fun refresh() {
        // The Flow will automatically re-emit, triggering loadPricesForHoldings
        // For manual refresh, we restart observation
        observeHoldings()
    }

    private fun observeHoldings() {
        viewModelScope.launch {
            holdingsRepository.getAllHoldings().collectLatest { holdings ->
                loadPricesForHoldings(holdings)
            }
        }
    }

    private suspend fun loadPricesForHoldings(holdings: List<HoldingEntity>) {
        if (holdings.isEmpty()) {
            _uiState.value = DashboardUiState.Success(emptyList())
            return
        }

        _uiState.value = DashboardUiState.Loading

        val symbols = holdings.map { it.symbol }
        val result = stockRepository.getQuotes(symbols)

        result.fold(
            onSuccess = { quotes ->
                val stocks = holdings.map { holding ->
                    val quote = quotes.find { it.symbol == holding.symbol }
                    val isKoreanStock = holding.symbol.endsWith(".KS") || holding.symbol.endsWith(".KQ")
                    Stock(
                        id = holding.id,
                        symbol = holding.symbol,
                        name = if (isKoreanStock) holding.name else (quote?.shortName ?: quote?.longName ?: holding.name),
                        quantity = holding.quantity,
                        averagePrice = holding.averagePrice,
                        currentPrice = quote?.regularMarketPrice ?: holding.averagePrice,
                        dayChange = quote?.regularMarketChange,
                        dayChangePercent = quote?.regularMarketChangePercent,
                        currency = holding.currency
                    )
                }
                _uiState.value = DashboardUiState.Success(stocks)
            },
            onFailure = { exception ->
                _uiState.value = DashboardUiState.Error(
                    exception.message ?: "Failed to load prices"
                )
            }
        )
    }
}
