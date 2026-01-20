package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val stocks: List<Stock>) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

data class Holding(
    val symbol: String,
    val name: String,
    val quantity: Int,
    val averagePrice: Double
)

class DashboardViewModel(
    private val repository: StockRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    // User's holdings (would come from local storage in production)
    private val holdings = listOf(
        Holding("AAPL", "Apple Inc.", 15, 145.00),
        Holding("GOOGL", "Alphabet Inc.", 8, 125.00),
        Holding("MSFT", "Microsoft Corp.", 12, 310.00),
        Holding("TSLA", "Tesla Inc.", 5, 280.00),
        Holding("NVDA", "NVIDIA Corp.", 10, 450.00),
        Holding("005930.KS", "삼성전자", 50, 72000.00),
        Holding("000660.KS", "SK하이닉스", 30, 125000.00)
    )

    init {
        loadPrices()
    }

    fun refresh() {
        loadPrices()
    }

    private fun loadPrices() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading

            val symbols = holdings.map { it.symbol }
            val result = repository.getQuotes(symbols)

            result.fold(
                onSuccess = { quotes ->
                    val stocks = holdings.map { holding ->
                        val quote = quotes.find { it.symbol == holding.symbol }
                        val isKoreanStock = holding.symbol.endsWith(".KS") || holding.symbol.endsWith(".KQ")
                        Stock(
                            symbol = holding.symbol,
                            name = if (isKoreanStock) holding.name else (quote?.shortName ?: quote?.longName ?: holding.name),
                            quantity = holding.quantity,
                            averagePrice = holding.averagePrice,
                            currentPrice = quote?.regularMarketPrice ?: holding.averagePrice,
                            dayChange = quote?.regularMarketChange,
                            dayChangePercent = quote?.regularMarketChangePercent,
                            currency = quote?.currency ?: "USD"
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
}
