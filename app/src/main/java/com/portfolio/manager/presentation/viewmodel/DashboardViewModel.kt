package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.SharedPreferences
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.StockAccountDetail
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.model.BenchmarkReturns
import com.portfolio.manager.domain.service.PortfolioStatsCalculator
import com.portfolio.manager.domain.service.PriceHistoryProcessor
import com.portfolio.manager.domain.service.StockHolding
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import com.portfolio.manager.util.AppConstants.DEFAULT_ACCOUNT_NAME
import com.portfolio.manager.util.AppConstants.KRW_TO_USD_RATE
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
import com.portfolio.manager.util.JsonSerializer
import kotlinx.serialization.encodeToString
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
        val benchmarkReturns: Map<TimePeriod, BenchmarkReturns> = emptyMap(),
        val selectedPeriod: TimePeriod = TimePeriod.ONE_YEAR,
        val isLoadingPeriodReturns: Boolean = false,
        val isRefreshing: Boolean = false,
        val exchangeRate: Double = KRW_TO_USD_RATE,
        val showInKrw: Boolean = false,
        val sparklinePeriod: TimePeriod = TimePeriod.ONE_YEAR,
        val portfolioSparkline: List<Double> = emptyList(),
        val portfolioStats: PortfolioStats = PortfolioStats()
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val holdingsRepository: HoldingsRepository,
    private val accountRepository: AccountRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    // Declare all properties BEFORE _uiState since loadCachedStateOrDefault() uses them
    private var selectedAccountId: Long = ALL_ACCOUNTS_ID
    private var holdingsJob: Job? = null
    private var currentExchangeRate: Double = KRW_TO_USD_RATE

    private var allAccountsCurrencyKrw: Boolean
        get() = sharedPreferences.getBoolean(PREF_DASHBOARD_SHOW_IN_KRW, false)
        set(value) = sharedPreferences.edit().putBoolean(PREF_DASHBOARD_SHOW_IN_KRW, value).apply()

    companion object {
        private const val PREF_DASHBOARD_SHOW_IN_KRW = "dashboard_show_in_krw"
        private const val PREF_DASHBOARD_CACHED_STOCKS_JSON = "dashboard_cached_stocks_json"
        private const val PREF_DASHBOARD_CACHED_EXCHANGE_RATE = "dashboard_cached_exchange_rate"
        private const val PREF_DASHBOARD_CACHED_PORTFOLIO_SPARKLINE = "dashboard_cached_portfolio_sparkline"
        private const val PREF_DASHBOARD_CACHED_PORTFOLIO_STATS = "dashboard_cached_portfolio_stats"
        private const val PREF_STOCK_SPARKLINE_PERIOD = "stock_sparkline_period"
        private const val PREF_PORTFOLIO_SUMMARY_PERIOD = "portfolio_summary_period"
        private const val BENCHMARK_SP500 = "^GSPC"
        private const val BENCHMARK_KOSPI = "^KS11"
    }

    private var sparklinePeriod: TimePeriod
        get() {
            val ordinal = sharedPreferences.getInt(PREF_STOCK_SPARKLINE_PERIOD, TimePeriod.ONE_YEAR.ordinal)
            return TimePeriod.entries.getOrElse(ordinal) { TimePeriod.ONE_YEAR }
        }
        set(value) = sharedPreferences.edit().putInt(PREF_STOCK_SPARKLINE_PERIOD, value.ordinal).apply()

    private var summaryPeriod: TimePeriod
        get() {
            val ordinal = sharedPreferences.getInt(PREF_PORTFOLIO_SUMMARY_PERIOD, TimePeriod.ONE_YEAR.ordinal)
            return TimePeriod.entries.getOrElse(ordinal) { TimePeriod.ONE_YEAR }
        }
        set(value) = sharedPreferences.edit().putInt(PREF_PORTFOLIO_SUMMARY_PERIOD, value.ordinal).apply()

    private val json = JsonSerializer.instance

    // Now initialize _uiState - all dependencies are ready
    private val _uiState = MutableStateFlow(loadCachedStateOrDefault())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private fun loadCachedStateOrDefault(): DashboardUiState {
        return try {
            val cachedStocksJson = sharedPreferences.getString(PREF_DASHBOARD_CACHED_STOCKS_JSON, null)
                ?: return DashboardUiState.Loading
            val cachedRate = sharedPreferences.getFloat(PREF_DASHBOARD_CACHED_EXCHANGE_RATE, KRW_TO_USD_RATE.toFloat()).toDouble()
            val stocks = json.decodeFromString<List<Stock>>(cachedStocksJson)
            currentExchangeRate = cachedRate

            val portfolioSparkline = sharedPreferences.getString(PREF_DASHBOARD_CACHED_PORTFOLIO_SPARKLINE, null)?.let {
                try { json.decodeFromString<List<Double>>(it) } catch (_: Exception) { emptyList() }
            } ?: emptyList()
            val portfolioStats = sharedPreferences.getString(PREF_DASHBOARD_CACHED_PORTFOLIO_STATS, null)?.let {
                try { json.decodeFromString<PortfolioStats>(it) } catch (_: Exception) { PortfolioStats() }
            } ?: PortfolioStats()

            DashboardUiState.Success(
                stocks = stocks,
                selectedAccountId = selectedAccountId,
                selectedPeriod = summaryPeriod,
                exchangeRate = cachedRate,
                showInKrw = allAccountsCurrencyKrw,
                isRefreshing = true,
                sparklinePeriod = sparklinePeriod,
                portfolioSparkline = portfolioSparkline,
                portfolioStats = portfolioStats
            )
        } catch (_: Exception) {
            DashboardUiState.Loading
        }
    }

    init {
        viewModelScope.launch {
            // Ensure default account exists (atomic operation to prevent race condition)
            accountRepository.getOrCreateDefaultAccount(DEFAULT_ACCOUNT_NAME)
            // Fetch exchange rate
            stockRepository.getExchangeRate("USD", "KRW").onSuccess { rate ->
                currentExchangeRate = rate
            }
            loadAndObserveHoldings()
        }
    }

    private fun saveStateToCache(stocks: List<Stock>, exchangeRate: Double) {
        try {
            val stocksJson = json.encodeToString(stocks)
            sharedPreferences.edit()
                .putString(PREF_DASHBOARD_CACHED_STOCKS_JSON, stocksJson)
                .putFloat(PREF_DASHBOARD_CACHED_EXCHANGE_RATE, exchangeRate.toFloat())
                .apply()
        } catch (_: Exception) {
            // Ignore cache errors
        }
    }

    fun selectAccount(accountId: Long) {
        selectedAccountId = accountId
        loadAndObserveHoldings()
    }

    fun toggleCurrency() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is DashboardUiState.Success) {
                val newShowInKrw = !currentState.showInKrw
                val newCurrency = if (newShowInKrw) "KRW" else "USD"

                if (selectedAccountId == ALL_ACCOUNTS_ID) {
                    allAccountsCurrencyKrw = newShowInKrw
                } else {
                    accountRepository.updatePreferredCurrency(selectedAccountId, newCurrency)
                }

                _uiState.value = currentState.copy(showInKrw = newShowInKrw)

                // Reload sparkline and period returns with new currency
                loadPeriodReturns(currentState.stocks, currentState.selectedPeriod)
                loadPortfolioSparkline(currentState.stocks, currentState.selectedPeriod)
            }
        }
    }

    fun refresh() {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.value = currentState.copy(isRefreshing = true)
        }
        loadAndObserveHoldings()
    }

    fun getSelectedAccountId(): Long = selectedAccountId

    fun deleteHolding(holdingId: Long) {
        viewModelScope.launch {
            holdingsRepository.deleteHolding(holdingId)
        }
    }

    private suspend fun getShowInKrwForCurrentAccount(): Boolean {
        return if (selectedAccountId == ALL_ACCOUNTS_ID) {
            allAccountsCurrencyKrw
        } else {
            accountRepository.getAccountById(selectedAccountId)?.preferredCurrency == "KRW"
        }
    }

    fun selectPeriod(period: TimePeriod) {
        summaryPeriod = period
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.value = currentState.copy(selectedPeriod = period)
            loadPeriodReturns(currentState.stocks, period)
            loadPortfolioSparkline(currentState.stocks, period)
        }
    }

    fun selectSparklinePeriod(period: TimePeriod) {
        sparklinePeriod = period
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.value = currentState.copy(sparklinePeriod = period, isRefreshing = true)
        }
        loadAndObserveHoldings()
    }

    private fun loadPeriodReturns(stocks: List<Stock>, period: TimePeriod) {
        if (stocks.isEmpty()) return

        viewModelScope.launch {
            val currentState = _uiState.value
            val showInKrw = (currentState as? DashboardUiState.Success)?.showInKrw ?: false
            if (currentState is DashboardUiState.Success) {
                _uiState.value = currentState.copy(isLoadingPeriodReturns = true)
            }

            val totalPortfolioValue = stocks.sumOf { it.totalValueInUsd(currentExchangeRate) }
            if (totalPortfolioValue <= 0) {
                updatePeriodReturn(period, 0.0, BenchmarkReturns(null, null))
                return@launch
            }

            // Fetch exchange rate history for currency-adjusted returns
            val exchangeRateData = stockRepository.getExchangeRateHistory("USD", "KRW", period.range)
            val startExchangeRate = exchangeRateData.prices.firstOrNull() ?: currentExchangeRate
            val endExchangeRate = exchangeRateData.prices.lastOrNull() ?: currentExchangeRate

            // Fetch period returns for all unique symbols in parallel, plus benchmarks
            val symbols = stocks.map { it.symbol }.distinct()
            val portfolioDeferred = symbols.map { symbol ->
                async { stockRepository.getPeriodReturn(symbol, period) }
            }
            val sp500Deferred = async { stockRepository.getPeriodReturn(BENCHMARK_SP500, period) }
            val kospiDeferred = async { stockRepository.getPeriodReturn(BENCHMARK_KOSPI, period) }

            val periodReturns = portfolioDeferred.awaitAll()
            val sp500Result = sp500Deferred.await()
            val kospiResult = kospiDeferred.await()

            // Create a map of symbol to return percent
            val returnsBySymbol = periodReturns
                .filter { it.isSuccess }
                .associate {
                    val periodReturn = it.getOrNull()!!
                    periodReturn.symbol to periodReturn.returnPercent
                }

            // Calculate weighted portfolio return with exchange rate consideration
            val weightedReturn = stocks.sumOf { stock ->
                val weight = stock.totalValueInUsd(currentExchangeRate) / totalPortfolioValue
                val stockReturn = returnsBySymbol[stock.symbol] ?: 0.0
                val adjustedReturn = PriceHistoryProcessor.adjustReturnForExchangeRate(
                    stockReturn, stock.currency, showInKrw, startExchangeRate, endExchangeRate
                )
                weight * adjustedReturn
            }

            // Build benchmark returns with currency adjustment
            val sp500Return = sp500Result.getOrNull()?.returnPercent?.let { rawReturn ->
                if (showInKrw) {
                    PriceHistoryProcessor.adjustReturnForExchangeRate(
                        rawReturn, "USD", showInKrw, startExchangeRate, endExchangeRate
                    )
                } else rawReturn
            }
            val kospiReturn = kospiResult.getOrNull()?.returnPercent?.let { rawReturn ->
                if (!showInKrw) {
                    PriceHistoryProcessor.adjustReturnForExchangeRate(
                        rawReturn, "KRW", showInKrw, startExchangeRate, endExchangeRate
                    )
                } else rawReturn
            }

            val benchmarks = BenchmarkReturns(sp500 = sp500Return, kospi = kospiReturn)
            updatePeriodReturn(period, weightedReturn, benchmarks)
        }
    }

    private fun updatePeriodReturn(period: TimePeriod, returnPercent: Double, benchmarks: BenchmarkReturns) {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            val updatedReturns = currentState.periodReturns.toMutableMap()
            updatedReturns[period] = returnPercent
            val updatedBenchmarks = currentState.benchmarkReturns.toMutableMap()
            updatedBenchmarks[period] = benchmarks
            _uiState.value = currentState.copy(
                periodReturns = updatedReturns,
                benchmarkReturns = updatedBenchmarks,
                isLoadingPeriodReturns = false
            )
        }
    }

    private fun loadPortfolioSparkline(stocks: List<Stock>, period: TimePeriod) {
        if (stocks.isEmpty()) return

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val showInKrw = (currentState as? DashboardUiState.Success)?.showInKrw ?: false

                val symbols = stocks.map { it.symbol }.distinct()
                val priceHistoryMap = stockRepository.getPriceHistory(symbols, period.range)
                val exchangeRateData = stockRepository.getExchangeRateHistory("USD", "KRW", period.range)

                val totalPortfolioValue = stocks.sumOf { it.totalValueInUsd(currentExchangeRate) }
                if (totalPortfolioValue <= 0) return@launch

                val stocksWithHistory = stocks.filter { stock ->
                    val data = priceHistoryMap[stock.symbol]
                    data != null && data.prices.size >= 2
                }
                if (stocksWithHistory.isEmpty()) {
                    updatePortfolioSparkline(emptyList())
                    return@launch
                }

                val symbolsWithHistory = stocksWithHistory.map { it.symbol }
                val stockDatePrices = PriceHistoryProcessor.buildSymbolDatePrices(symbolsWithHistory, priceHistoryMap)
                val exchangeRateByDate = PriceHistoryProcessor.buildExchangeRateByDate(exchangeRateData)
                val allDates = stockDatePrices.values.flatMap { it.keys }.toSet().sorted()

                if (allDates.size < 2) {
                    updatePortfolioSparkline(emptyList())
                    return@launch
                }

                val filledStockPrices = PriceHistoryProcessor.forwardFillPrices(symbolsWithHistory, stockDatePrices, allDates)
                val validDates = allDates.filter { date ->
                    stocksWithHistory.all { stock ->
                        filledStockPrices[stock.symbol]?.containsKey(date) == true
                    }
                }

                val holdings = stocksWithHistory.map { StockHolding(it.symbol, it.quantity, it.currency) }
                val portfolioValues = PriceHistoryProcessor.calculatePortfolioValues(
                    validDates, holdings, filledStockPrices, exchangeRateByDate, currentExchangeRate, showInKrw
                )

                if (portfolioValues.size < 2) {
                    updatePortfolioSparkline(emptyList(), PortfolioStats())
                    return@launch
                }

                val stats = PortfolioStatsCalculator.calculate(portfolioValues)
                updatePortfolioSparkline(PriceHistoryProcessor.normalizeValues(portfolioValues), stats)
            } catch (e: Exception) {
                updatePortfolioSparkline(emptyList(), PortfolioStats())
            }
        }
    }

    private fun updatePortfolioSparkline(sparkline: List<Double>, stats: PortfolioStats = PortfolioStats()) {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.value = currentState.copy(
                portfolioSparkline = sparkline,
                portfolioStats = stats
            )
            // Cache portfolio sparkline for fast cold start (only for All Accounts with valid data)
            if (selectedAccountId == ALL_ACCOUNTS_ID && sparkline.isNotEmpty()) {
                try {
                    sharedPreferences.edit()
                        .putString(PREF_DASHBOARD_CACHED_PORTFOLIO_SPARKLINE, json.encodeToString(sparkline))
                        .putString(PREF_DASHBOARD_CACHED_PORTFOLIO_STATS, json.encodeToString(stats))
                        .apply()
                } catch (_: Exception) {
                    // Ignore cache errors
                }
            }
        }
    }

    private fun loadAndObserveHoldings() {
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
            val showInKrw = getShowInKrwForCurrentAccount()
            _uiState.value = DashboardUiState.Success(
                stocks = emptyList(),
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                exchangeRate = currentExchangeRate,
                showInKrw = showInKrw,
                sparklinePeriod = sparklinePeriod
            )
            return
        }

        val currentState = _uiState.value
        val currentSuccess = currentState as? DashboardUiState.Success
        val isRefreshing = currentSuccess?.isRefreshing == true

        val symbols = holdings.map { it.symbol }.distinct()
        val existingStocks = currentSuccess?.stocks ?: emptyList()
        val existingSymbols = existingStocks.map { it.symbol }.toSet()
        val newSymbols = symbols.filter { it !in existingSymbols }

        // If we have existing data and no new symbols, just merge holdings with existing prices
        if (currentSuccess != null && newSymbols.isEmpty() && !isRefreshing) {
            val updatedStocks = mergeHoldingsWithExistingStocks(holdings, existingStocks, allAccounts)
            _uiState.value = currentSuccess.copy(
                stocks = updatedStocks,
                accounts = accounts,
                selectedAccountId = selectedAccountId
            )
            // Cache and update portfolio calculations
            if (selectedAccountId == ALL_ACCOUNTS_ID) {
                saveStateToCache(updatedStocks, currentExchangeRate)
            }
            loadPeriodReturns(updatedStocks, currentSuccess.selectedPeriod)
            loadPortfolioSparkline(updatedStocks, currentSuccess.selectedPeriod)
            return
        }

        // Full refresh needed: new symbols, manual refresh, or no existing data
        if (!isRefreshing && currentSuccess == null) {
            _uiState.value = DashboardUiState.Loading
        }

        val result = stockRepository.getQuotes(symbols)

        result.fold(
            onSuccess = { quotes ->
                // Fetch price history for sparklines (non-blocking, failures return empty)
                val priceHistoryMap = stockRepository.getPriceHistory(symbols, sparklinePeriod.range)

                val stocks = if (selectedAccountId == ALL_ACCOUNTS_ID) {
                    // Aggregate holdings by symbol when viewing all accounts
                    aggregateHoldings(holdings, quotes, allAccounts, priceHistoryMap)
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
                            currency = holding.currency,
                            priceHistory = priceHistoryMap[holding.symbol]?.prices ?: emptyList()
                        )
                    }
                }.sortedByDescending { it.totalValueInUsd(currentExchangeRate) }
                val previousReturns = currentSuccess?.periodReturns ?: emptyMap()
                val selectedPeriod = currentSuccess?.selectedPeriod ?: summaryPeriod
                val showInKrw = getShowInKrwForCurrentAccount()
                _uiState.value = DashboardUiState.Success(
                    stocks = stocks,
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    periodReturns = previousReturns,
                    selectedPeriod = selectedPeriod,
                    isRefreshing = false,
                    exchangeRate = currentExchangeRate,
                    showInKrw = showInKrw,
                    sparklinePeriod = sparklinePeriod,
                    portfolioSparkline = currentSuccess?.portfolioSparkline ?: emptyList(),
                    portfolioStats = currentSuccess?.portfolioStats ?: PortfolioStats()
                )
                // Cache state for fast cold start (only for All Accounts view)
                if (selectedAccountId == ALL_ACCOUNTS_ID) {
                    saveStateToCache(stocks, currentExchangeRate)
                }
                // Load period returns and portfolio sparkline for the selected period
                loadPeriodReturns(stocks, selectedPeriod)
                loadPortfolioSparkline(stocks, selectedPeriod)
            },
            onFailure = { exception ->
                _uiState.value = DashboardUiState.Error(
                    exception.message ?: "Failed to load prices"
                )
            }
        )
    }

    private fun mergeHoldingsWithExistingStocks(
        holdings: List<HoldingEntity>,
        existingStocks: List<Stock>,
        allAccounts: List<AccountEntity>
    ): List<Stock> {
        val existingStockMap = existingStocks.associateBy { it.symbol }
        val accountMap = allAccounts.associateBy { it.id }
        val accountOrderMap = allAccounts.associate { it.id to it.orderIndex }

        return if (selectedAccountId == ALL_ACCOUNTS_ID) {
            // Aggregate holdings by symbol
            holdings.groupBy { it.symbol }.map { (symbol, holdingGroup) ->
                val existingStock = existingStockMap[symbol]
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

                Stock(
                    id = holdingGroup.first().id,
                    symbol = symbol,
                    name = existingStock?.name ?: symbol,
                    quantity = totalQuantity,
                    averagePrice = weightedAvgPrice,
                    currentPrice = existingStock?.currentPrice ?: weightedAvgPrice,
                    dayChange = existingStock?.dayChange,
                    dayChangePercent = existingStock?.dayChangePercent,
                    currency = holdingGroup.first().currency,
                    accountDetails = accountDetails,
                    priceHistory = existingStock?.priceHistory ?: emptyList()
                )
            }
        } else {
            // Single account view
            holdings.map { holding ->
                val existingStock = existingStockMap[holding.symbol]
                Stock(
                    id = holding.id,
                    symbol = holding.symbol,
                    name = existingStock?.name ?: holding.symbol,
                    quantity = holding.quantity,
                    averagePrice = holding.averagePrice,
                    currentPrice = existingStock?.currentPrice ?: holding.averagePrice,
                    dayChange = existingStock?.dayChange,
                    dayChangePercent = existingStock?.dayChangePercent,
                    currency = holding.currency,
                    priceHistory = existingStock?.priceHistory ?: emptyList()
                )
            }
        }.sortedByDescending { it.totalValueInUsd(currentExchangeRate) }
    }

    private fun aggregateHoldings(
        holdings: List<HoldingEntity>,
        quotes: List<com.portfolio.manager.data.remote.dto.QuoteResult>,
        accounts: List<AccountEntity>,
        priceHistoryMap: Map<String, PriceHistoryData>
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
                accountDetails = accountDetails,
                priceHistory = priceHistoryMap[symbol]?.prices ?: emptyList()
            )
        }
    }
}
