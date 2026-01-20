package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.StockAccountDetail
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import com.portfolio.manager.util.AppConstants.DEFAULT_ACCOUNT_NAME
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountWithCount(
    val account: AccountEntity,
    val holdingsCount: Int
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val stocks: List<Stock>,
        val accounts: List<AccountWithCount> = emptyList(),
        val selectedAccountId: Long = 1L,
        val periodReturns: Map<TimePeriod, Double> = emptyMap(),
        val selectedPeriod: TimePeriod = TimePeriod.ONE_DAY,
        val isLoadingPeriodReturns: Boolean = false,
        val isRefreshing: Boolean = false
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val holdingsRepository: HoldingsRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var selectedAccountId: Long = ALL_ACCOUNTS_ID
    private var holdingsJob: Job? = null

    init {
        viewModelScope.launch {
            // Ensure default account exists (atomic operation to prevent race condition)
            accountRepository.getOrCreateDefaultAccount(DEFAULT_ACCOUNT_NAME)
            observeHoldings()
        }
    }

    fun selectAccount(accountId: Long) {
        selectedAccountId = accountId
        observeHoldings()
    }

    fun refresh() {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.value = currentState.copy(isRefreshing = true)
        }
        observeHoldings()
    }

    fun getSelectedAccountId(): Long = selectedAccountId

    fun deleteHolding(holdingId: Long) {
        viewModelScope.launch {
            holdingsRepository.deleteHolding(holdingId)
        }
    }

    fun selectPeriod(period: TimePeriod) {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.value = currentState.copy(selectedPeriod = period)
            loadPeriodReturns(currentState.stocks, period)
        }
    }

    private fun loadPeriodReturns(stocks: List<Stock>, period: TimePeriod) {
        if (stocks.isEmpty()) return

        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is DashboardUiState.Success) {
                _uiState.value = currentState.copy(isLoadingPeriodReturns = true)
            }

            val totalPortfolioValue = stocks.sumOf { it.totalValueInUsd }
            if (totalPortfolioValue <= 0) {
                updatePeriodReturn(period, 0.0)
                return@launch
            }

            // Fetch period returns for all unique symbols in parallel
            val symbols = stocks.map { it.symbol }.distinct()
            val periodReturns = symbols.map { symbol ->
                async { stockRepository.getPeriodReturn(symbol, period) }
            }.awaitAll()

            // Create a map of symbol to return percent
            val returnsBySymbol = periodReturns
                .filter { it.isSuccess }
                .associate {
                    val periodReturn = it.getOrNull()!!
                    periodReturn.symbol to periodReturn.returnPercent
                }

            // Calculate weighted portfolio return
            val weightedReturn = stocks.sumOf { stock ->
                val weight = stock.totalValueInUsd / totalPortfolioValue
                val stockReturn = returnsBySymbol[stock.symbol] ?: 0.0
                weight * stockReturn
            }

            updatePeriodReturn(period, weightedReturn)
        }
    }

    private fun updatePeriodReturn(period: TimePeriod, returnPercent: Double) {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            val updatedReturns = currentState.periodReturns.toMutableMap()
            updatedReturns[period] = returnPercent
            _uiState.value = currentState.copy(
                periodReturns = updatedReturns,
                isLoadingPeriodReturns = false
            )
        }
    }

    private fun observeHoldings() {
        holdingsJob?.cancel()
        holdingsJob = viewModelScope.launch {
            val holdingsFlow = if (selectedAccountId == ALL_ACCOUNTS_ID) {
                holdingsRepository.getAllHoldings()
            } else {
                holdingsRepository.getHoldingsByAccount(selectedAccountId)
            }

            combine(
                holdingsFlow,
                accountRepository.getAllAccounts(),
                holdingsRepository.getHoldingsCountByAccountFlow()
            ) { holdings, accounts, countMap ->
                Triple(holdings, accounts, countMap)
            }.collectLatest { (holdings, accounts, countMap) ->
                val accountsWithCount = accounts.map { account ->
                    AccountWithCount(
                        account = account,
                        holdingsCount = countMap[account.id] ?: 0
                    )
                }
                loadPricesForHoldings(holdings, accountsWithCount, accounts)
            }
        }
    }

    private suspend fun loadPricesForHoldings(
        holdings: List<HoldingEntity>,
        accounts: List<AccountWithCount>,
        allAccounts: List<AccountEntity>
    ) {
        if (holdings.isEmpty()) {
            _uiState.value = DashboardUiState.Success(
                stocks = emptyList(),
                accounts = accounts,
                selectedAccountId = selectedAccountId
            )
            return
        }

        val currentState = _uiState.value
        val isRefreshing = currentState is DashboardUiState.Success && currentState.isRefreshing
        if (!isRefreshing) {
            _uiState.value = DashboardUiState.Loading
        }

        val symbols = holdings.map { it.symbol }.distinct()
        val result = stockRepository.getQuotes(symbols)

        result.fold(
            onSuccess = { quotes ->
                val stocks = if (selectedAccountId == ALL_ACCOUNTS_ID) {
                    // Aggregate holdings by symbol when viewing all accounts
                    aggregateHoldings(holdings, quotes, allAccounts)
                } else {
                    // Normal view for single account
                    holdings.map { holding ->
                        val quote = quotes.find { it.symbol == holding.symbol }
                        val stockName = quote?.longName ?: quote?.shortName ?: holding.symbol
                        Stock(
                            id = holding.id,
                            symbol = holding.symbol,
                            name = stockName,
                            quantity = holding.quantity,
                            averagePrice = holding.averagePrice,
                            currentPrice = quote?.regularMarketPrice ?: holding.averagePrice,
                            dayChange = quote?.regularMarketChange,
                            dayChangePercent = quote?.regularMarketChangePercent,
                            currency = holding.currency
                        )
                    }
                }.sortedByDescending { it.totalValueInUsd }
                val previousReturns = if (currentState is DashboardUiState.Success) {
                    currentState.periodReturns
                } else {
                    emptyMap()
                }
                val selectedPeriod = if (currentState is DashboardUiState.Success) {
                    currentState.selectedPeriod
                } else {
                    TimePeriod.ONE_DAY
                }
                _uiState.value = DashboardUiState.Success(
                    stocks = stocks,
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    periodReturns = previousReturns,
                    selectedPeriod = selectedPeriod,
                    isRefreshing = false
                )
                // Load period returns for the selected period
                loadPeriodReturns(stocks, selectedPeriod)
            },
            onFailure = { exception ->
                _uiState.value = DashboardUiState.Error(
                    exception.message ?: "Failed to load prices"
                )
            }
        )
    }

    private fun aggregateHoldings(
        holdings: List<HoldingEntity>,
        quotes: List<com.portfolio.manager.data.remote.dto.QuoteResult>,
        accounts: List<AccountEntity>
    ): List<Stock> {
        val accountMap = accounts.associateBy { it.id }
        val accountOrderMap = accounts.associate { it.id to it.orderIndex }

        return holdings.groupBy { it.symbol }.map { (symbol, holdingGroup) ->
            val quote = quotes.find { it.symbol == symbol }
            val firstHolding = holdingGroup.first()

            val totalQuantity = holdingGroup.sumOf { it.quantity }
            val totalCost = holdingGroup.sumOf { it.quantity * it.averagePrice }
            val weightedAvgPrice = if (totalQuantity > 0) totalCost / totalQuantity else 0.0

            val accountDetails = holdingGroup.map { holding ->
                StockAccountDetail(
                    holdingId = holding.id,
                    accountId = holding.accountId,
                    accountName = accountMap[holding.accountId]?.name ?: "Unknown",
                    quantity = holding.quantity,
                    averagePrice = holding.averagePrice
                )
            }.sortedBy { accountOrderMap[it.accountId] ?: Int.MAX_VALUE }

            val stockName = quote?.longName ?: quote?.shortName ?: symbol

            Stock(
                id = firstHolding.id,
                symbol = symbol,
                name = stockName,
                quantity = totalQuantity,
                averagePrice = weightedAvgPrice,
                currentPrice = quote?.regularMarketPrice ?: weightedAvgPrice,
                dayChange = quote?.regularMarketChange,
                dayChangePercent = quote?.regularMarketChangePercent,
                currency = firstHolding.currency,
                accountDetails = accountDetails
            )
        }
    }
}
