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
import com.portfolio.manager.domain.model.SortOption
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
import com.portfolio.manager.util.boolean
import com.portfolio.manager.util.enum
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
        val portfolioSparklineTimestamps: List<Long> = emptyList(),
        val benchmarkSparklines: Map<String, List<Double>> = emptyMap(),
        val portfolioStats: PortfolioStats = PortfolioStats(),
        val sortOption: SortOption = SortOption.WEIGHT
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

    private var allAccountsCurrencyKrw by sharedPreferences.boolean(PREF_DASHBOARD_SHOW_IN_KRW, false)

    companion object {
        private const val PREF_DASHBOARD_SHOW_IN_KRW = "dashboard_show_in_krw"
        private const val PREF_DASHBOARD_CACHED_STOCKS_JSON = "dashboard_cached_stocks_json"
        private const val PREF_DASHBOARD_CACHED_EXCHANGE_RATE = "dashboard_cached_exchange_rate"
        private const val PREF_DASHBOARD_CACHED_PORTFOLIO_SPARKLINE = "dashboard_cached_portfolio_sparkline"
        private const val PREF_DASHBOARD_CACHED_PORTFOLIO_STATS = "dashboard_cached_portfolio_stats"
        private const val PREF_STOCK_SPARKLINE_PERIOD = "stock_sparkline_period"
        private const val PREF_PORTFOLIO_SUMMARY_PERIOD = "portfolio_summary_period"
        private const val PREF_SORT_OPTION = "sort_option"
        private const val BENCHMARK_SP500 = "^GSPC"
        private const val BENCHMARK_KOSPI = "^KS11"
    }

    private var sparklinePeriod by sharedPreferences.enum(PREF_STOCK_SPARKLINE_PERIOD, TimePeriod.ONE_YEAR)

    private var summaryPeriod by sharedPreferences.enum(PREF_PORTFOLIO_SUMMARY_PERIOD, TimePeriod.ONE_YEAR)

    private var currentSortOption by sharedPreferences.enum(PREF_SORT_OPTION, SortOption.WEIGHT)

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
                portfolioStats = portfolioStats,
                sortOption = currentSortOption
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
                if (currentState.stocks.isNotEmpty()) {
                    loadBenchmarkReturns(currentState.selectedPeriod)
                }
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
            if (currentState.stocks.isNotEmpty()) {
                loadBenchmarkReturns(period)
            }
            loadPortfolioSparkline(currentState.stocks, period)
        }
    }

    fun selectSparklinePeriod(period: TimePeriod) {
        sparklinePeriod = period
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.value = currentState.copy(sparklinePeriod = period, isRefreshing = true)
            reloadStockSparklines(currentState.stocks, period)
        }
    }

    private fun reloadStockSparklines(stocks: List<Stock>, period: TimePeriod) {
        viewModelScope.launch {
            val symbols = stocks.map { it.symbol }.distinct()
            val priceHistoryMap = stockRepository.getPriceHistory(symbols, period.range)

            val updatedStocks = stocks.map { stock ->
                stock.copy(
                    priceHistory = priceHistoryMap[stock.symbol]?.prices ?: emptyList(),
                    priceHistoryTimestamps = priceHistoryMap[stock.symbol]?.timestamps ?: emptyList()
                )
            }

            val currentState = _uiState.value
            if (currentState is DashboardUiState.Success) {
                _uiState.value = currentState.copy(
                    stocks = updatedStocks,
                    isRefreshing = false
                )
            }
        }
    }

    fun selectSortOption(option: SortOption) {
        currentSortOption = option
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            val sortedStocks = sortStocks(currentState.stocks, option)
            _uiState.value = currentState.copy(stocks = sortedStocks, sortOption = option)
        }
    }

    private fun sortStocks(stocks: List<Stock>, option: SortOption): List<Stock> {
        return when (option) {
            SortOption.WEIGHT -> stocks.sortedByDescending { it.totalValueInUsd(currentExchangeRate) }
            SortOption.NAME -> stocks.sortedBy { it.name.lowercase() }
            SortOption.SYMBOL -> stocks.sortedBy { it.symbol.lowercase() }
            SortOption.GAIN_LOSS_PERCENT -> stocks.sortedByDescending { it.gainLossPercent }
            SortOption.DAY_CHANGE_PERCENT -> stocks.sortedByDescending { it.dayChangePercent ?: 0.0 }
        }
    }

    private fun loadBenchmarkReturns(period: TimePeriod) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val showInKrw = (currentState as? DashboardUiState.Success)?.showInKrw ?: false

            // Fetch exchange rate history for currency-adjusted returns
            val exchangeRateData = stockRepository.getExchangeRateHistory("USD", "KRW", period.range)
            val startExchangeRate = exchangeRateData.prices.firstOrNull() ?: currentExchangeRate
            val endExchangeRate = exchangeRateData.prices.lastOrNull() ?: currentExchangeRate

            // Fetch benchmark price histories
            val sp500Deferred = async { stockRepository.getPriceHistory(listOf(BENCHMARK_SP500), period.range) }
            val kospiDeferred = async { stockRepository.getPriceHistory(listOf(BENCHMARK_KOSPI), period.range) }

            val sp500History = sp500Deferred.await()[BENCHMARK_SP500]
            val kospiHistory = kospiDeferred.await()[BENCHMARK_KOSPI]

            // Calculate benchmark returns from price history (same as sparkline)
            val sp500Return = sp500History?.prices?.let { prices ->
                PriceHistoryProcessor.calculateBenchmarkReturn(
                    prices, "USD", showInKrw, startExchangeRate, endExchangeRate
                )
            }
            val kospiReturn = kospiHistory?.prices?.let { prices ->
                PriceHistoryProcessor.calculateBenchmarkReturn(
                    prices, "KRW", showInKrw, startExchangeRate, endExchangeRate
                )
            }

            val benchmarks = BenchmarkReturns(sp500 = sp500Return, kospi = kospiReturn)
            updateBenchmarkReturns(period, benchmarks)
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

    private fun updateBenchmarkReturns(period: TimePeriod, benchmarks: BenchmarkReturns) {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            val updatedBenchmarks = currentState.benchmarkReturns.toMutableMap()
            updatedBenchmarks[period] = benchmarks
            _uiState.value = currentState.copy(benchmarkReturns = updatedBenchmarks)
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
                    updatePeriodReturn(period, 0.0)
                    return@launch
                }

                val symbolsWithHistory = stocksWithHistory.map { it.symbol }
                val stockDatePrices = PriceHistoryProcessor.buildSymbolDatePrices(symbolsWithHistory, priceHistoryMap)
                val exchangeRateByDate = PriceHistoryProcessor.buildExchangeRateByDate(exchangeRateData)
                val allDates = stockDatePrices.values.flatMap { it.keys }.toSet().sorted()

                if (allDates.size < 2) {
                    updatePortfolioSparkline(emptyList())
                    updatePeriodReturn(period, 0.0)
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
                    updatePortfolioSparkline(emptyList(), stats = PortfolioStats())
                    updatePeriodReturn(period, 0.0)
                    return@launch
                }

                // Convert date strings to timestamps for chart interaction
                val timestamps = validDates.mapNotNull { PriceHistoryProcessor.dateToTimestamp(it) }

                val stats = PortfolioStatsCalculator.calculate(portfolioValues)
                val normalizedPortfolio = PriceHistoryProcessor.normalizeValues(portfolioValues)

                val benchmarkSparklines = fetchBenchmarkSparklines(period)

                updatePortfolioSparkline(
                    sparkline = normalizedPortfolio,
                    timestamps = timestamps,
                    stats = stats,
                    benchmarkSparklines = benchmarkSparklines
                )

                val periodReturn = calculatePeriodReturnPercent(portfolioValues)
                updatePeriodReturn(period, periodReturn)
            } catch (e: Exception) {
                updatePortfolioSparkline(emptyList(), stats = PortfolioStats())
            }
        }
    }

    private suspend fun fetchBenchmarkSparklines(period: TimePeriod): Map<String, List<Double>> {
        val sp500History = stockRepository.getPriceHistory(listOf(BENCHMARK_SP500), period.range)[BENCHMARK_SP500]
        val kospiHistory = stockRepository.getPriceHistory(listOf(BENCHMARK_KOSPI), period.range)[BENCHMARK_KOSPI]

        return buildMap {
            sp500History?.prices?.takeIf { it.size >= 2 }?.let { prices ->
                put(BENCHMARK_SP500, PriceHistoryProcessor.normalizeValues(prices))
            }
            kospiHistory?.prices?.takeIf { it.size >= 2 }?.let { prices ->
                put(BENCHMARK_KOSPI, PriceHistoryProcessor.normalizeValues(prices))
            }
        }
    }

    private fun calculatePeriodReturnPercent(portfolioValues: List<Double>): Double {
        if (portfolioValues.size < 2) return 0.0
        val startValue = portfolioValues.first()
        val endValue = portfolioValues.last()
        return if (startValue > 0) ((endValue - startValue) / startValue) * 100 else 0.0
    }

    private fun updatePortfolioSparkline(
        sparkline: List<Double>,
        timestamps: List<Long> = emptyList(),
        stats: PortfolioStats = PortfolioStats(),
        benchmarkSparklines: Map<String, List<Double>> = emptyMap()
    ) {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.value = currentState.copy(
                portfolioSparkline = sparkline,
                portfolioSparklineTimestamps = timestamps,
                benchmarkSparklines = benchmarkSparklines,
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
                sparklinePeriod = sparklinePeriod,
                sortOption = currentSortOption
            )
            return
        }

        val currentState = _uiState.value
        val currentSuccess = currentState as? DashboardUiState.Success
        val isRefreshing = currentSuccess?.isRefreshing == true

        // Update accounts immediately if we have cached state (for cold start)
        if (currentSuccess != null && currentSuccess.accounts.isEmpty() && accounts.isNotEmpty()) {
            _uiState.value = currentSuccess.copy(accounts = accounts)
        }

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
            loadBenchmarkReturns(currentSuccess.selectedPeriod)
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
                            priceHistory = priceHistoryMap[holding.symbol]?.prices ?: emptyList(),
                            priceHistoryTimestamps = priceHistoryMap[holding.symbol]?.timestamps ?: emptyList(),
                            annualDividend = quote?.trailingAnnualDividendRate,
                            dividendYield = quote?.trailingAnnualDividendYield?.let { it * 100 }
                        )
                    }
                }
                val sortedStocks = sortStocks(stocks, currentSortOption)
                val previousReturns = currentSuccess?.periodReturns ?: emptyMap()
                val selectedPeriod = currentSuccess?.selectedPeriod ?: summaryPeriod
                val showInKrw = getShowInKrwForCurrentAccount()
                _uiState.value = DashboardUiState.Success(
                    stocks = sortedStocks,
                    accounts = accounts,
                    selectedAccountId = selectedAccountId,
                    periodReturns = previousReturns,
                    selectedPeriod = selectedPeriod,
                    isRefreshing = false,
                    exchangeRate = currentExchangeRate,
                    showInKrw = showInKrw,
                    sparklinePeriod = sparklinePeriod,
                    portfolioSparkline = currentSuccess?.portfolioSparkline ?: emptyList(),
                    portfolioStats = currentSuccess?.portfolioStats ?: PortfolioStats(),
                    sortOption = currentSortOption
                )
                // Cache state for fast cold start (only for All Accounts view)
                if (selectedAccountId == ALL_ACCOUNTS_ID) {
                    saveStateToCache(stocks, currentExchangeRate)
                }
                // Load period returns and portfolio sparkline for the selected period
                loadBenchmarkReturns(selectedPeriod)
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
                    priceHistory = existingStock?.priceHistory ?: emptyList(),
                    priceHistoryTimestamps = existingStock?.priceHistoryTimestamps ?: emptyList(),
                    annualDividend = existingStock?.annualDividend,
                    dividendYield = existingStock?.dividendYield
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
                    priceHistory = existingStock?.priceHistory ?: emptyList(),
                    priceHistoryTimestamps = existingStock?.priceHistoryTimestamps ?: emptyList(),
                    annualDividend = existingStock?.annualDividend,
                    dividendYield = existingStock?.dividendYield
                )
            }
        }.let { sortStocks(it, currentSortOption) }
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
                priceHistory = priceHistoryMap[symbol]?.prices ?: emptyList(),
                priceHistoryTimestamps = priceHistoryMap[symbol]?.timestamps ?: emptyList(),
                annualDividend = quote?.trailingAnnualDividendRate,
                dividendYield = quote?.trailingAnnualDividendYield?.let { it * 100 }
            )
        }
    }

    fun canShowRebalance(): Boolean {
        val state = _uiState.value as? DashboardUiState.Success ?: return false
        return selectedAccountId != ALL_ACCOUNTS_ID && state.stocks.isNotEmpty()
    }

    suspend fun getRebalanceItems(): List<RebalanceItemData> {
        val state = _uiState.value as? DashboardUiState.Success ?: return emptyList()
        if (selectedAccountId == ALL_ACCOUNTS_ID) return emptyList()

        val holdings = holdingsRepository.getHoldingsByAccountSync(selectedAccountId)
        val stocks = state.stocks

        return stocks.mapNotNull { stock ->
            val holding = holdings.find { it.symbol == stock.symbol } ?: return@mapNotNull null
            val value = if (state.showInKrw) {
                stock.totalValueInKrw(currentExchangeRate)
            } else {
                stock.totalValueInUsd(currentExchangeRate)
            }
            val price = if (state.showInKrw) {
                if (stock.currency == "KRW") stock.currentPrice else stock.currentPrice * currentExchangeRate
            } else {
                if (stock.currency == "USD") stock.currentPrice else stock.currentPrice / currentExchangeRate
            }
            RebalanceItemData(
                holdingId = holding.id,
                symbol = stock.symbol,
                name = stock.name,
                currentValue = value,
                currentPrice = price,
                currentPercentage = holding.targetPercentage ?: 0,
                currency = stock.currency
            )
        }
    }

    fun saveTargetPercentages(percentages: Map<Long, Int>) {
        viewModelScope.launch {
            percentages.forEach { (holdingId, percentage) ->
                holdingsRepository.updateTargetPercentage(holdingId, percentage)
            }
        }
    }

    fun getTotalPortfolioValue(): Double {
        val state = _uiState.value as? DashboardUiState.Success ?: return 0.0
        return if (state.showInKrw) {
            state.stocks.sumOf { it.totalValueInKrw(currentExchangeRate) }
        } else {
            state.stocks.sumOf { it.totalValueInUsd(currentExchangeRate) }
        }
    }

    fun isShowingInKrw(): Boolean {
        return (_uiState.value as? DashboardUiState.Success)?.showInKrw ?: false
    }
}

data class RebalanceItemData(
    val holdingId: Long,
    val symbol: String,
    val name: String,
    val currentValue: Double,
    val currentPrice: Double,
    val currentPercentage: Int,
    val currency: String
)
