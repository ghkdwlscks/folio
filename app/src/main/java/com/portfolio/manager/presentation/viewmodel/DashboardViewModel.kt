package com.portfolio.manager.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.SharedPreferences
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.model.BenchmarkReturns
import com.portfolio.manager.domain.model.SortOption
import com.portfolio.manager.domain.service.CacheManager
import com.portfolio.manager.domain.service.PortfolioSorter
import com.portfolio.manager.domain.service.PortfolioStatsCalculator
import com.portfolio.manager.domain.service.PriceHistoryProcessor
import com.portfolio.manager.domain.service.StockHolding
import com.portfolio.manager.domain.service.StockMapper
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.CashRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.presentation.util.CurrencyConverter
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import com.portfolio.manager.util.AppConstants.BENCHMARK_KOSPI
import com.portfolio.manager.util.AppConstants.BENCHMARK_SP500
import com.portfolio.manager.util.AppConstants.DEFAULT_ACCOUNT_NAME
import com.portfolio.manager.util.AppConstants.KRW_TO_USD_RATE
import com.portfolio.manager.util.PreferenceKeys
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
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val holdingsRepository: HoldingsRepository,
    private val accountRepository: AccountRepository,
    private val cashRepository: CashRepository,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    // Declare all properties BEFORE _uiState since loadCachedStateOrDefault() uses them
    private var selectedAccountId: Long = ALL_ACCOUNTS_ID
    private var holdingsJob: Job? = null
    private var currentExchangeRate: Double = KRW_TO_USD_RATE

    private var allAccountsCurrencyKrw by sharedPreferences.boolean(PreferenceKeys.DASHBOARD_SHOW_IN_KRW, true)

    private var sparklinePeriod by sharedPreferences.enum(PreferenceKeys.STOCK_SPARKLINE_PERIOD, TimePeriod.ONE_YEAR)

    private var summaryPeriod by sharedPreferences.enum(PreferenceKeys.PORTFOLIO_SUMMARY_PERIOD, TimePeriod.ONE_YEAR)

    private var currentSortOption by sharedPreferences.enum(PreferenceKeys.SORT_OPTION, SortOption.WEIGHT)

    private val json = JsonSerializer.instance
    private val cacheManager = CacheManager(sharedPreferences)

    // Now initialize _uiState - all dependencies are ready
    private val _uiState = MutableStateFlow(loadCachedStateOrDefault())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /**
     * Helper function to update the UI state if it's currently in Success state.
     * Reduces boilerplate for state updates.
     */
    private inline fun updateSuccessState(
        block: (DashboardUiState.Success) -> DashboardUiState.Success
    ) {
        val current = _uiState.value as? DashboardUiState.Success ?: return
        _uiState.value = block(current)
    }

    private fun loadCachedStateOrDefault(): DashboardUiState {
        return try {
            val stocks = cacheManager.load<List<Stock>>(PreferenceKeys.DASHBOARD_CACHED_STOCKS_JSON)
                ?: return DashboardUiState.Loading
            val cachedRate = cacheManager.loadFloat(PreferenceKeys.DASHBOARD_CACHED_EXCHANGE_RATE, KRW_TO_USD_RATE.toFloat()).toDouble()
            currentExchangeRate = cachedRate

            val cashItems = cacheManager.loadOrDefault<List<CashItem>>(PreferenceKeys.DASHBOARD_CACHED_CASH_ITEMS_JSON, emptyList())
            val accounts = cacheManager.loadOrDefault<List<AccountWithCount>>(PreferenceKeys.DASHBOARD_CACHED_ACCOUNTS_JSON, emptyList())
            val portfolioSparkline = cacheManager.loadOrDefault<List<Double>>(PreferenceKeys.DASHBOARD_CACHED_PORTFOLIO_SPARKLINE, emptyList())
            val portfolioStats = cacheManager.loadOrDefault<PortfolioStats>(PreferenceKeys.DASHBOARD_CACHED_PORTFOLIO_STATS, PortfolioStats())
            val periodReturns = cacheManager.load<Map<String, Double>>(PreferenceKeys.DASHBOARD_CACHED_PERIOD_RETURNS)
                ?.mapKeys { (key, _) -> TimePeriod.valueOf(key) }
                ?: emptyMap()

            DashboardUiState.Success(
                stocks = stocks,
                cashItems = cashItems,
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                periodReturns = periodReturns,
                selectedPeriod = summaryPeriod,
                exchangeRate = cachedRate,
                showInKrw = allAccountsCurrencyKrw,
                isRefreshing = true,
                sparklinePeriod = sparklinePeriod,
                portfolioSparkline = portfolioSparkline,
                portfolioStats = portfolioStats,
                sortOption = currentSortOption
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached state, starting fresh", e)
            DashboardUiState.Loading
        }
    }

    override fun onCleared() {
        super.onCleared()
        holdingsJob?.cancel()
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

    private fun saveStateToCache(stocks: List<Stock>, cashItems: List<CashItem>, accounts: List<AccountWithCount>, exchangeRate: Double) {
        cacheManager.saveMultiple {
            put(PreferenceKeys.DASHBOARD_CACHED_STOCKS_JSON, stocks)
            put(PreferenceKeys.DASHBOARD_CACHED_CASH_ITEMS_JSON, cashItems)
            put(PreferenceKeys.DASHBOARD_CACHED_ACCOUNTS_JSON, accounts)
            putFloat(PreferenceKeys.DASHBOARD_CACHED_EXCHANGE_RATE, exchangeRate.toFloat())
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
                loadPortfolioSparkline(currentState.stocks, currentState.cashItems, currentState.selectedPeriod)
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

    fun addCashItem(accountId: Long, name: String, value: Double, yieldRate: Double, currency: String) {
        viewModelScope.launch {
            cashRepository.addCashItem(accountId, name, value, yieldRate, currency)
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
            loadPortfolioSparkline(currentState.stocks, currentState.cashItems, period)
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
            val sortedCashItems = sortCashItems(currentState.cashItems, option)
            _uiState.value = currentState.copy(
                stocks = sortedStocks,
                cashItems = sortedCashItems,
                sortOption = option
            )
        }
    }

    private fun sortStocks(stocks: List<Stock>, option: SortOption): List<Stock> =
        PortfolioSorter.sortStocks(stocks, option, currentExchangeRate)

    private fun sortCashItems(cashItems: List<CashItem>, option: SortOption): List<CashItem> =
        PortfolioSorter.sortCashItems(cashItems, option, currentExchangeRate)

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
        updateSuccessState { state ->
            val updatedReturns = state.periodReturns.toMutableMap()
            updatedReturns[period] = returnPercent
            state.copy(periodReturns = updatedReturns, isLoadingPeriodReturns = false)
        }
    }

    private fun updateBenchmarkReturns(period: TimePeriod, benchmarks: BenchmarkReturns) {
        updateSuccessState { state ->
            val updatedBenchmarks = state.benchmarkReturns.toMutableMap()
            updatedBenchmarks[period] = benchmarks
            state.copy(benchmarkReturns = updatedBenchmarks)
        }
    }

    private fun loadPortfolioSparkline(stocks: List<Stock>, cashItems: List<CashItem>, period: TimePeriod) {
        if (stocks.isEmpty() && cashItems.isEmpty()) return

        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val showInKrw = (currentState as? DashboardUiState.Success)?.showInKrw ?: false

                // Calculate constant cash value (doesn't change over time)
                val totalCashValue = if (showInKrw) {
                    cashItems.sumOf { it.valueInKrw(currentExchangeRate) }
                } else {
                    cashItems.sumOf { it.valueInUsd(currentExchangeRate) }
                }

                // If only cash (no stocks), we can't show a sparkline (no price history)
                if (stocks.isEmpty()) {
                    updatePortfolioSparkline(emptyList(), stats = PortfolioStats())
                    return@launch
                }

                val symbols = stocks.map { it.symbol }.distinct()
                val priceHistoryMap = stockRepository.getPriceHistory(symbols, period.range)
                val exchangeRateData = stockRepository.getExchangeRateHistory("USD", "KRW", period.range)

                val totalStocksValue = stocks.sumOf { it.totalValueInUsd(currentExchangeRate) }
                if (totalStocksValue <= 0 && totalCashValue <= 0) return@launch

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

                val filledExchangeRates = PriceHistoryProcessor.forwardFillExchangeRates(
                    exchangeRateByDate, validDates, currentExchangeRate
                )
                val holdings = stocksWithHistory.map { StockHolding(it.symbol, it.quantity, it.currency) }
                val stockPortfolioValues = PriceHistoryProcessor.calculatePortfolioValues(
                    validDates, holdings, filledStockPrices, filledExchangeRates, currentExchangeRate, showInKrw
                )

                // Add cash value to each portfolio value point
                val portfolioValues = stockPortfolioValues.map { it + totalCashValue }

                if (portfolioValues.size < 2) {
                    updatePortfolioSparkline(emptyList(), stats = PortfolioStats())
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
                // Period returns are calculated by loadAllPeriodReturns() which includes cash
            } catch (e: Exception) {
                updatePortfolioSparkline(emptyList(), stats = PortfolioStats())
            }
        }
    }

    private fun loadAllPeriodReturns(stocks: List<Stock>, cashItems: List<CashItem> = emptyList()) {
        if (stocks.isEmpty() && cashItems.isEmpty()) return

        viewModelScope.launch {
            val currentState = _uiState.value
            val showInKrw = (currentState as? DashboardUiState.Success)?.showInKrw ?: false

            // Calculate total cash value in USD
            val totalCashValueUsd = cashItems.sumOf { it.valueInUsd(currentExchangeRate) }

            // If only cash, use cash returns directly
            if (stocks.isEmpty()) {
                TimePeriod.entries.forEach { period ->
                    val cashReturn = calculateWeightedCashReturn(cashItems, period)
                    updatePeriodReturn(period, cashReturn)
                }
                if (selectedAccountId == ALL_ACCOUNTS_ID) {
                    savePeriodReturnsToCache()
                }
                return@launch
            }

            val symbols = stocks.map { it.symbol }.distinct()
            val stocksForCalc = stocks.filter { it.totalValueInUsd(currentExchangeRate) > 0 }
            if (stocksForCalc.isEmpty() && cashItems.isEmpty()) return@launch

            val totalStocksValueUsd = stocksForCalc.sumOf { it.totalValueInUsd(currentExchangeRate) }
            val totalPortfolioValue = totalStocksValueUsd + totalCashValueUsd
            val stocksWeight = if (totalPortfolioValue > 0) totalStocksValueUsd / totalPortfolioValue else 1.0
            val cashWeight = if (totalPortfolioValue > 0) totalCashValueUsd / totalPortfolioValue else 0.0

            // Load returns for all periods in parallel
            TimePeriod.entries.map { period ->
                async {
                    try {
                        val priceHistoryMap = stockRepository.getPriceHistory(symbols, period.range)
                        val exchangeRateData = stockRepository.getExchangeRateHistory("USD", "KRW", period.range)

                        val stocksWithHistory = stocksForCalc.filter { stock ->
                            val data = priceHistoryMap[stock.symbol]
                            data != null && data.prices.size >= 2
                        }

                        val stocksReturn = if (stocksWithHistory.isEmpty()) {
                            0.0
                        } else {
                            val symbolsWithHistory = stocksWithHistory.map { it.symbol }
                            val stockDatePrices = PriceHistoryProcessor.buildSymbolDatePrices(symbolsWithHistory, priceHistoryMap)
                            val exchangeRateByDate = PriceHistoryProcessor.buildExchangeRateByDate(exchangeRateData)
                            val allDates = stockDatePrices.values.flatMap { it.keys }.toSet().sorted()

                            if (allDates.size < 2) {
                                0.0
                            } else {
                                val filledStockPrices = PriceHistoryProcessor.forwardFillPrices(symbolsWithHistory, stockDatePrices, allDates)
                                val validDates = allDates.filter { date ->
                                    stocksWithHistory.all { stock ->
                                        filledStockPrices[stock.symbol]?.containsKey(date) == true
                                    }
                                }

                                val filledExchangeRates = PriceHistoryProcessor.forwardFillExchangeRates(
                                    exchangeRateByDate, validDates, currentExchangeRate
                                )
                                val holdings = stocksWithHistory.map { StockHolding(it.symbol, it.quantity, it.currency) }
                                val portfolioValues = PriceHistoryProcessor.calculatePortfolioValues(
                                    validDates, holdings, filledStockPrices, filledExchangeRates, currentExchangeRate, showInKrw
                                )

                                calculatePeriodReturnPercent(portfolioValues)
                            }
                        }

                        // Calculate weighted average of cash returns for this period
                        val cashReturn = calculateWeightedCashReturn(cashItems, period)

                        // Combined return = stocks weight * stocks return + cash weight * cash return
                        val combinedReturn = (stocksWeight * stocksReturn) + (cashWeight * cashReturn)
                        period to combinedReturn
                    } catch (e: Exception) {
                        period to 0.0
                    }
                }
            }.awaitAll().forEach { (period, returnPercent) ->
                updatePeriodReturn(period, returnPercent)
            }

            // Cache period returns for fast cold start (only for All Accounts view)
            if (selectedAccountId == ALL_ACCOUNTS_ID) {
                savePeriodReturnsToCache()
            }
        }
    }

    private fun calculateWeightedCashReturn(cashItems: List<CashItem>, period: TimePeriod): Double {
        if (cashItems.isEmpty()) return 0.0
        val totalCashValue = cashItems.sumOf { it.valueInUsd(currentExchangeRate) }
        if (totalCashValue <= 0) return 0.0

        return cashItems.sumOf { cashItem ->
            val weight = cashItem.valueInUsd(currentExchangeRate) / totalCashValue
            weight * cashItem.periodReturnPercent(period)
        }
    }

    private fun savePeriodReturnsToCache() {
        val currentState = _uiState.value as? DashboardUiState.Success ?: return
        if (currentState.periodReturns.isEmpty()) return
        val stringKeyMap = currentState.periodReturns.mapKeys { (key, _) -> key.name }
        cacheManager.save(PreferenceKeys.DASHBOARD_CACHED_PERIOD_RETURNS, stringKeyMap)
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
        updateSuccessState { state ->
            state.copy(
                portfolioSparkline = sparkline,
                portfolioSparklineTimestamps = timestamps,
                benchmarkSparklines = benchmarkSparklines,
                portfolioStats = stats
            )
        }
        // Cache portfolio sparkline for fast cold start (only for All Accounts with valid data)
        if (selectedAccountId == ALL_ACCOUNTS_ID && sparkline.isNotEmpty()) {
            cacheManager.saveMultiple {
                put(PreferenceKeys.DASHBOARD_CACHED_PORTFOLIO_SPARKLINE, sparkline)
                put(PreferenceKeys.DASHBOARD_CACHED_PORTFOLIO_STATS, stats)
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

            val cashFlow = if (selectedAccountId == ALL_ACCOUNTS_ID) {
                cashRepository.getAllCashItems()
            } else {
                cashRepository.getCashItemsByAccount(selectedAccountId)
            }

            combine(
                holdingsFlow,
                cashFlow,
                accountRepository.getAllAccounts(),
                holdingsRepository.getHoldingsCountByAccountFlow(),
                cashRepository.getAllCashItems() // For counting cash items per account
            ) { holdings, cashEntities, accounts, holdingsCountMap, allCashEntities ->
                val cashItems = cashEntities.map { CashItem.fromEntity(it) }
                // Calculate cash items count per account
                val cashCountMap = allCashEntities.groupBy { it.accountId }.mapValues { it.value.size }
                HoldingsData(holdings, cashItems, accounts, holdingsCountMap, cashCountMap)
            }.collectLatest { data ->
                val accountsWithCount = data.accounts.map { account ->
                    val holdingsCount = data.holdingsCountMap[account.id] ?: 0
                    val cashCount = data.cashCountMap[account.id] ?: 0
                    AccountWithCount(
                        account = account,
                        holdingsCount = holdingsCount + cashCount
                    )
                }
                loadPricesForHoldings(data.holdings, data.cashItems, accountsWithCount, data.accounts)
            }
        }
    }

    private data class HoldingsData(
        val holdings: List<HoldingEntity>,
        val cashItems: List<CashItem>,
        val accounts: List<AccountEntity>,
        val holdingsCountMap: Map<Long, Int>,
        val cashCountMap: Map<Long, Int>
    )

    private suspend fun loadPricesForHoldings(
        holdings: List<HoldingEntity>,
        cashItems: List<CashItem>,
        accounts: List<AccountWithCount>,
        allAccounts: List<AccountEntity>
    ) {
        if (holdings.isEmpty() && cashItems.isEmpty()) {
            val showInKrw = getShowInKrwForCurrentAccount()
            val currentSuccess = _uiState.value as? DashboardUiState.Success
            _uiState.value = DashboardUiState.Success(
                stocks = emptyList(),
                cashItems = emptyList(),
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                periodReturns = currentSuccess?.periodReturns ?: emptyMap(),
                selectedPeriod = currentSuccess?.selectedPeriod ?: summaryPeriod,
                exchangeRate = currentExchangeRate,
                showInKrw = showInKrw,
                sparklinePeriod = sparklinePeriod,
                portfolioSparkline = currentSuccess?.portfolioSparkline ?: emptyList(),
                portfolioStats = currentSuccess?.portfolioStats ?: PortfolioStats(),
                sortOption = currentSortOption
            )
            return
        }

        // Handle case where we only have cash items (no holdings)
        if (holdings.isEmpty()) {
            val showInKrw = getShowInKrwForCurrentAccount()
            val currentSuccess = _uiState.value as? DashboardUiState.Success
            val sortedCashItems = sortCashItems(cashItems, currentSortOption)
            _uiState.value = DashboardUiState.Success(
                stocks = emptyList(),
                cashItems = sortedCashItems,
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                periodReturns = currentSuccess?.periodReturns ?: emptyMap(),
                selectedPeriod = currentSuccess?.selectedPeriod ?: summaryPeriod,
                exchangeRate = currentExchangeRate,
                showInKrw = showInKrw,
                sparklinePeriod = sparklinePeriod,
                portfolioSparkline = emptyList(),
                portfolioStats = PortfolioStats(),
                sortOption = currentSortOption
            )
            // Calculate period returns for cash-only portfolio
            loadAllPeriodReturns(emptyList(), sortedCashItems)
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
            val sortedCashItems = sortCashItems(cashItems, currentSortOption)
            _uiState.value = currentSuccess.copy(
                stocks = updatedStocks,
                cashItems = sortedCashItems,
                accounts = accounts,
                selectedAccountId = selectedAccountId
            )
            // Cache and update portfolio calculations
            if (selectedAccountId == ALL_ACCOUNTS_ID) {
                saveStateToCache(updatedStocks, sortedCashItems, accounts, currentExchangeRate)
            }
            loadBenchmarkReturns(currentSuccess.selectedPeriod)
            loadPortfolioSparkline(updatedStocks, sortedCashItems, currentSuccess.selectedPeriod)
            loadAllPeriodReturns(updatedStocks, sortedCashItems)
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
                        StockMapper.createStock(holding, quote, priceHistoryMap[holding.symbol])
                    }
                }
                val sortedStocks = sortStocks(stocks, currentSortOption)
                val sortedCashItems = sortCashItems(cashItems, currentSortOption)
                val previousReturns = currentSuccess?.periodReturns ?: emptyMap()
                val selectedPeriod = currentSuccess?.selectedPeriod ?: summaryPeriod
                val showInKrw = getShowInKrwForCurrentAccount()
                _uiState.value = DashboardUiState.Success(
                    stocks = sortedStocks,
                    cashItems = sortedCashItems,
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
                    saveStateToCache(sortedStocks, sortedCashItems, accounts, currentExchangeRate)
                }
                // Load period returns for all periods and portfolio sparkline for selected period
                loadAllPeriodReturns(stocks, sortedCashItems)
                loadBenchmarkReturns(selectedPeriod)
                loadPortfolioSparkline(stocks, sortedCashItems, selectedPeriod)
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

        return if (selectedAccountId == ALL_ACCOUNTS_ID) {
            // Aggregate holdings by symbol
            holdings.groupBy { it.symbol }.map { (symbol, holdingGroup) ->
                StockMapper.mergeAggregatedWithExisting(holdingGroup, existingStockMap[symbol], allAccounts)
            }
        } else {
            // Single account view
            holdings.map { holding ->
                StockMapper.mergeWithExisting(holding, existingStockMap[holding.symbol])
            }
        }.let { sortStocks(it, currentSortOption) }
    }

    private fun aggregateHoldings(
        holdings: List<HoldingEntity>,
        quotes: List<com.portfolio.manager.data.remote.dto.QuoteResult>,
        accounts: List<AccountEntity>,
        priceHistoryMap: Map<String, PriceHistoryData>
    ): List<Stock> {
        return holdings.groupBy { it.symbol }.map { (symbol, holdingGroup) ->
            val quote = quotes.find { it.symbol == symbol }
            StockMapper.aggregateHoldings(holdingGroup, quote, accounts, priceHistoryMap[symbol])
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
            val price = CurrencyConverter.convert(stock.currentPrice, stock.currency, state.showInKrw, currentExchangeRate)
            RebalanceItemData(
                holdingId = holding.id,
                symbol = stock.symbol,
                name = stock.name,
                currentValue = value,
                currentPrice = price,
                currentPercentage = holding.targetPercentage ?: 0,
                currency = stock.currency,
                quantity = stock.quantity
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

    fun getStocksValue(): Double {
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

    fun deleteCashItem(cashItemId: Long) {
        viewModelScope.launch {
            cashRepository.deleteCashItem(cashItemId)
        }
    }
}

data class RebalanceItemData(
    val holdingId: Long,
    val symbol: String,
    val name: String,
    val currentValue: Double,
    val currentPrice: Double,
    val currentPercentage: Int,
    val currency: String,
    val quantity: Int
)
