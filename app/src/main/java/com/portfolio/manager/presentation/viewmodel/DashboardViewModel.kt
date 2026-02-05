package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences
import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.BenchmarkReturns
import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.SortOption
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.CashRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.domain.service.BenchmarkDataService
import com.portfolio.manager.domain.service.CacheManager
import com.portfolio.manager.domain.service.CashItemMapper
import com.portfolio.manager.domain.service.PeriodReturnsService
import com.portfolio.manager.domain.service.PortfolioCache
import com.portfolio.manager.domain.service.PortfolioSorter
import com.portfolio.manager.domain.service.RebalanceCalculator
import com.portfolio.manager.domain.service.RebalanceItemData
import com.portfolio.manager.domain.service.SparklineService
import com.portfolio.manager.domain.service.StockMapper
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import com.portfolio.manager.util.AppConstants.DEFAULT_ACCOUNT_NAME
import com.portfolio.manager.util.AppConstants.KRW_TO_USD_RATE
import com.portfolio.manager.util.ErrorMessages
import com.portfolio.manager.util.JsonSerializer
import com.portfolio.manager.util.PreferenceKeys
import com.portfolio.manager.util.boolean
import com.portfolio.manager.util.enum

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val holdingsRepository: HoldingsRepository,
    private val accountRepository: AccountRepository,
    private val cashRepository: CashRepository,
    private val sharedPreferences: SharedPreferences,
    private val portfolioCache: PortfolioCache
) : ViewModel() {

    companion object {
        private const val TAG = "DashboardViewModel"
    }

    // Declare all properties BEFORE _uiState since loadCachedStateOrDefault() uses them
    private var selectedAccountId: Long = ALL_ACCOUNTS_ID
    private var holdingsJob: Job? = null
    private var currentExchangeRate: Double = KRW_TO_USD_RATE

    // In-memory cache of all stocks (loaded from SharedPreferences, updated on API fetch)
    private var allStocksCache: List<Stock> = emptyList()

    // Per-account cache for portfolio data (avoids showing wrong account's data during switch)
    private val portfolioSparklineCache = mutableMapOf<Long, List<Double>>()
    private val periodReturnsCache = mutableMapOf<Long, Map<TimePeriod, Double>>()
    private val portfolioStatsCache = mutableMapOf<Long, PortfolioStats>()

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
            // Load into memory cache for fast account switching
            allStocksCache = stocks
            val cachedRate = cacheManager.loadFloat(PreferenceKeys.DASHBOARD_CACHED_EXCHANGE_RATE, KRW_TO_USD_RATE.toFloat()).toDouble()
            currentExchangeRate = cachedRate

            val cashItems = cacheManager.loadOrDefault<List<CashItem>>(PreferenceKeys.DASHBOARD_CACHED_CASH_ITEMS_JSON, emptyList())

            // Update shared portfolio cache for FIRE calculator (same data shown in Dashboard)
            portfolioCache.update(stocks, cashItems, cachedRate)

            val accounts = cacheManager.loadOrDefault<List<AccountWithCount>>(PreferenceKeys.DASHBOARD_CACHED_ACCOUNTS_JSON, emptyList())
            val portfolioSparkline = cacheManager.loadOrDefault<List<Double>>(PreferenceKeys.DASHBOARD_CACHED_PORTFOLIO_SPARKLINE, emptyList())
            val portfolioSparklineTimestamps = cacheManager.loadOrDefault<List<Long>>(PreferenceKeys.DASHBOARD_CACHED_SPARKLINE_TIMESTAMPS, emptyList())
            val portfolioStats = cacheManager.loadOrDefault<PortfolioStats>(PreferenceKeys.DASHBOARD_CACHED_PORTFOLIO_STATS, PortfolioStats())
            val periodReturns = cacheManager.load<Map<String, Double>>(PreferenceKeys.DASHBOARD_CACHED_PERIOD_RETURNS)
                ?.mapKeys { (key, _) -> TimePeriod.valueOf(key) }
                ?: emptyMap()
            val benchmarkSparklines = cacheManager.loadOrDefault<Map<String, List<Double>>>(
                PreferenceKeys.DASHBOARD_CACHED_BENCHMARK_SPARKLINES, emptyMap()
            )
            val benchmarkTimestamps = cacheManager.loadOrDefault<Map<String, List<Long>>>(
                PreferenceKeys.DASHBOARD_CACHED_BENCHMARK_TIMESTAMPS, emptyMap()
            )
            val benchmarkReturns = cacheManager.load<Map<String, BenchmarkReturns>>(
                PreferenceKeys.DASHBOARD_CACHED_BENCHMARK_RETURNS
            )?.mapKeys { (key, _) -> TimePeriod.valueOf(key) } ?: emptyMap()

            // Load into per-account caches for ALL_ACCOUNTS_ID (default view on cold start)
            if (portfolioSparkline.isNotEmpty()) {
                portfolioSparklineCache[ALL_ACCOUNTS_ID] = portfolioSparkline
                portfolioStatsCache[ALL_ACCOUNTS_ID] = portfolioStats
            }
            if (periodReturns.isNotEmpty()) {
                periodReturnsCache[ALL_ACCOUNTS_ID] = periodReturns
            }

            DashboardUiState.Success(
                stocks = stocks,
                cashItems = cashItems,
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                periodReturns = periodReturns,
                benchmarkReturns = benchmarkReturns,
                selectedPeriod = summaryPeriod,
                exchangeRate = cachedRate,
                showInKrw = allAccountsCurrencyKrw,
                isRefreshing = true,
                sparklinePeriod = sparklinePeriod,
                portfolioSparkline = portfolioSparkline,
                portfolioSparklineTimestamps = portfolioSparklineTimestamps,
                portfolioStats = portfolioStats,
                benchmarkSparklines = benchmarkSparklines,
                benchmarkTimestamps = benchmarkTimestamps,
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
        // Update shared portfolio cache for FIRE calculator
        portfolioCache.update(stocks, cashItems, exchangeRate)

        // Persist to SharedPreferences (in-memory cache already updated in handleFullRefresh)
        cacheManager.saveMultiple {
            put(PreferenceKeys.DASHBOARD_CACHED_STOCKS_JSON, stocks)
            put(PreferenceKeys.DASHBOARD_CACHED_CASH_ITEMS_JSON, cashItems)
            put(PreferenceKeys.DASHBOARD_CACHED_ACCOUNTS_JSON, accounts)
            putFloat(PreferenceKeys.DASHBOARD_CACHED_EXCHANGE_RATE, exchangeRate.toFloat())
        }
    }


    fun selectAccount(accountId: Long) {
        selectedAccountId = accountId
        // Render immediately from cache, then refresh in background
        renderFromCacheThenRefresh()
    }

    private fun renderFromCacheThenRefresh() {
        holdingsJob?.cancel()
        holdingsJob = viewModelScope.launch {
            observeHoldingsData().collectLatest { data ->
                val accountsWithCount = data.toAccountsWithCount()

                // First: render immediately from in-memory cache (contains all stocks)
                val cachedStocks = if (allStocksCache.isNotEmpty() && data.holdings.isNotEmpty()) {
                    renderFromCachedStocks(data.holdings, data.cashItems, accountsWithCount, data.accounts, allStocksCache)
                } else null

                // Load portfolio sparkline, benchmark, and period returns from cached stocks (don't wait for API)
                if (cachedStocks != null) {
                    loadPortfolioSparkline(cachedStocks, data.cashItems, summaryPeriod)
                    loadBenchmarkData(summaryPeriod)
                    loadAllPeriodReturns(cachedStocks, data.cashItems)
                }

                // Then: refresh from API in background
                loadPricesForHoldings(data.holdings, data.cashItems, accountsWithCount, data.accounts)
            }
        }
    }

    private suspend fun renderFromCachedStocks(
        holdings: List<HoldingEntity>,
        cashItems: List<CashItem>,
        accounts: List<AccountWithCount>,
        allAccounts: List<AccountEntity>,
        cachedStocks: List<Stock>
    ): List<Stock>? {
        val symbols = holdings.map { it.symbol }.distinct()
        val cachedStockMap = cachedStocks.associateBy { it.symbol }

        // Only render if we have cached data for all symbols
        if (symbols.any { it !in cachedStockMap }) return null

        val showInKrw = getShowInKrwForCurrentAccount()

        // Build stocks using cached prices (reuse existing merge methods)
        val stocks = if (selectedAccountId == ALL_ACCOUNTS_ID) {
            holdings.groupBy { it.symbol }.map { (symbol, holdingGroup) ->
                StockMapper.mergeAggregatedWithExisting(holdingGroup, cachedStockMap[symbol], allAccounts)
            }
        } else {
            holdings.map { holding ->
                StockMapper.mergeWithExisting(holding, cachedStockMap[holding.symbol])
            }
        }

        val sortedStocks = sortStocks(stocks, currentSortOption)
        val sortedCashItems = sortCashItems(cashItems, currentSortOption)

        // Calculate rebalance status for accounts
        val accountsWithRebalance = calculateAccountsWithRebalanceStatus(accounts, showInKrw)

        // Use per-account cached data (avoids showing wrong account's data)
        _uiState.value = DashboardUiState.Success(
            stocks = sortedStocks,
            cashItems = sortedCashItems,
            accounts = accountsWithRebalance,
            selectedAccountId = selectedAccountId,
            periodReturns = periodReturnsCache[selectedAccountId] ?: emptyMap(),
            selectedPeriod = summaryPeriod,
            isRefreshing = true,
            exchangeRate = currentExchangeRate,
            showInKrw = showInKrw,
            sparklinePeriod = sparklinePeriod,
            portfolioSparkline = portfolioSparklineCache[selectedAccountId] ?: emptyList(),
            portfolioStats = portfolioStatsCache[selectedAccountId] ?: PortfolioStats(),
            sortOption = currentSortOption
        )

        return sortedStocks
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

                // Clear cached period returns for this account (will be recalculated with new currency)
                periodReturnsCache.remove(selectedAccountId)
                _uiState.value = currentState.copy(showInKrw = newShowInKrw, periodReturns = emptyMap())

                // Reload sparkline, benchmark, and period returns with new currency
                if (currentState.stocks.isNotEmpty()) {
                    loadBenchmarkData(currentState.selectedPeriod)
                }
                loadPortfolioSparkline(currentState.stocks, currentState.cashItems, currentState.selectedPeriod)
                loadAllPeriodReturns(currentState.stocks, currentState.cashItems)
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
            val entity = CashItemEntity(
                accountId = accountId,
                name = name,
                originalValue = value,
                annualYieldRate = yieldRate,
                currency = currency
            )
            cashRepository.addCashItem(entity)
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
                loadBenchmarkData(period)
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

            // Update in-memory cache with new sparklines
            val cacheMap = allStocksCache.associateBy { it.symbol }.toMutableMap()
            updatedStocks.forEach { stock -> cacheMap[stock.symbol] = stock }
            allStocksCache = cacheMap.values.toList()

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

    private fun loadBenchmarkData(period: TimePeriod) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val showInKrw = (currentState as? DashboardUiState.Success)?.showInKrw ?: false

            val result = BenchmarkDataService.loadBenchmarkData(
                stockRepository, period, showInKrw, currentExchangeRate
            )
            updateBenchmarkData(period, result.returns, result.sparklines, result.timestamps)
        }
    }

    private fun updatePeriodReturn(period: TimePeriod, returnPercent: Double) {
        // Save to per-account cache for fast account switching
        val accountCache = periodReturnsCache.getOrPut(selectedAccountId) { mutableMapOf() }.toMutableMap()
        accountCache[period] = returnPercent
        periodReturnsCache[selectedAccountId] = accountCache

        updateSuccessState { state ->
            // Skip if value hasn't changed (avoids unnecessary re-render)
            if (state.periodReturns[period] == returnPercent) {
                return@updateSuccessState state
            }
            val updatedReturns = state.periodReturns.toMutableMap()
            updatedReturns[period] = returnPercent
            state.copy(periodReturns = updatedReturns, isLoadingPeriodReturns = false)
        }
    }

    private fun updateBenchmarkData(
        period: TimePeriod,
        benchmarks: BenchmarkReturns,
        sparklines: Map<String, List<Double>>,
        timestamps: Map<String, List<Long>>
    ) {
        updateSuccessState { state ->
            // Skip if data is the same (avoids unnecessary re-render)
            if (state.benchmarkReturns[period] == benchmarks &&
                state.benchmarkSparklines == sparklines &&
                state.benchmarkTimestamps == timestamps
            ) {
                return@updateSuccessState state
            }
            val updatedBenchmarks = state.benchmarkReturns.toMutableMap()
            updatedBenchmarks[period] = benchmarks
            state.copy(
                benchmarkReturns = updatedBenchmarks,
                benchmarkSparklines = sparklines,
                benchmarkTimestamps = timestamps
            )
        }
        // Cache benchmark data for fast cold start (only for All Accounts)
        if (selectedAccountId == ALL_ACCOUNTS_ID && sparklines.isNotEmpty()) {
            val benchmarkReturnsMap = mapOf(period.name to benchmarks)
            cacheManager.saveMultiple {
                put(PreferenceKeys.DASHBOARD_CACHED_BENCHMARK_SPARKLINES, sparklines)
                put(PreferenceKeys.DASHBOARD_CACHED_BENCHMARK_TIMESTAMPS, timestamps)
                put(PreferenceKeys.DASHBOARD_CACHED_BENCHMARK_RETURNS, benchmarkReturnsMap)
            }
        }
    }

    private fun loadPortfolioSparkline(stocks: List<Stock>, cashItems: List<CashItem>, period: TimePeriod) {
        if (stocks.isEmpty() && cashItems.isEmpty()) return

        viewModelScope.launch {
            val currentState = _uiState.value
            val showInKrw = (currentState as? DashboardUiState.Success)?.showInKrw ?: false

            val result = SparklineService.calculateSparkline(
                stockRepository, stocks, cashItems, period, showInKrw, currentExchangeRate
            )
            updatePortfolioSparkline(result.sparkline, result.timestamps, result.stats)
        }
    }

    private fun loadAllPeriodReturns(stocks: List<Stock>, cashItems: List<CashItem> = emptyList()) {
        if (stocks.isEmpty() && cashItems.isEmpty()) return

        viewModelScope.launch {
            val currentState = _uiState.value
            val showInKrw = (currentState as? DashboardUiState.Success)?.showInKrw ?: false

            val periodReturns = PeriodReturnsService.calculateAllPeriodReturns(
                stockRepository, stocks, cashItems, showInKrw, currentExchangeRate
            )
            periodReturns.forEach { (period, returnPercent) ->
                updatePeriodReturn(period, returnPercent)
            }

            if (selectedAccountId == ALL_ACCOUNTS_ID) {
                savePeriodReturnsToCache()
            }
        }
    }

    private fun savePeriodReturnsToCache() {
        val currentState = _uiState.value as? DashboardUiState.Success ?: return
        if (currentState.periodReturns.isEmpty()) return
        val stringKeyMap = currentState.periodReturns.mapKeys { (key, _) -> key.name }
        cacheManager.save(PreferenceKeys.DASHBOARD_CACHED_PERIOD_RETURNS, stringKeyMap)
    }

    private fun updatePortfolioSparkline(
        sparkline: List<Double>,
        timestamps: List<Long> = emptyList(),
        stats: PortfolioStats = PortfolioStats()
    ) {
        // Save to per-account cache for fast account switching
        if (sparkline.isNotEmpty()) {
            portfolioSparklineCache[selectedAccountId] = sparkline
            portfolioStatsCache[selectedAccountId] = stats
        }

        updateSuccessState { state ->
            // Don't replace valid data with empty data (avoids blink)
            if (sparkline.isEmpty() && state.portfolioSparkline.isNotEmpty()) {
                return@updateSuccessState state
            }
            // Skip if data is the same (avoids unnecessary re-render)
            if (sparkline == state.portfolioSparkline && timestamps == state.portfolioSparklineTimestamps && stats == state.portfolioStats) {
                return@updateSuccessState state
            }
            state.copy(
                portfolioSparkline = sparkline,
                portfolioSparklineTimestamps = timestamps,
                portfolioStats = stats
            )
        }
        // Cache portfolio sparkline for fast cold start (only for All Accounts with valid data)
        if (selectedAccountId == ALL_ACCOUNTS_ID && sparkline.isNotEmpty()) {
            cacheManager.saveMultiple {
                put(PreferenceKeys.DASHBOARD_CACHED_PORTFOLIO_SPARKLINE, sparkline)
                put(PreferenceKeys.DASHBOARD_CACHED_SPARKLINE_TIMESTAMPS, timestamps)
                put(PreferenceKeys.DASHBOARD_CACHED_PORTFOLIO_STATS, stats)
            }
        }
    }

    private fun loadAndObserveHoldings() {
        holdingsJob?.cancel()
        holdingsJob = viewModelScope.launch {
            observeHoldingsData().collectLatest { data ->
                loadPricesForHoldings(data.holdings, data.cashItems, data.toAccountsWithCount(), data.accounts)
            }
        }
    }

    private data class HoldingsData(
        val holdings: List<HoldingEntity>,
        val cashItems: List<CashItem>,
        val accounts: List<AccountEntity>,
        val holdingsCountMap: Map<Long, Int>,
        val cashCountMap: Map<Long, Int>
    ) {
        fun toAccountsWithCount(): List<AccountWithCount> = accounts.map { account ->
            val holdingsCount = holdingsCountMap[account.id] ?: 0
            val cashCount = cashCountMap[account.id] ?: 0
            AccountWithCount(account = account, holdingsCount = holdingsCount + cashCount)
        }
    }

    private fun observeHoldingsData() = combine(
        if (selectedAccountId == ALL_ACCOUNTS_ID) holdingsRepository.getAllHoldings()
        else holdingsRepository.getHoldingsByAccount(selectedAccountId),
        if (selectedAccountId == ALL_ACCOUNTS_ID) cashRepository.getAllCashItems()
        else cashRepository.getCashItemsByAccount(selectedAccountId),
        accountRepository.getAllAccounts(),
        holdingsRepository.getHoldingsCountByAccountFlow(),
        cashRepository.getCashItemsCountByAccountFlow()
    ) { holdings, cashEntities, accounts, holdingsCountMap, cashCountMap ->
        val cashItems = CashItemMapper.fromEntities(cashEntities)
        HoldingsData(holdings, cashItems, accounts, holdingsCountMap, cashCountMap)
    }

    private suspend fun loadPricesForHoldings(
        holdings: List<HoldingEntity>,
        cashItems: List<CashItem>,
        accounts: List<AccountWithCount>,
        allAccounts: List<AccountEntity>
    ) {
        if (holdings.isEmpty() && cashItems.isEmpty()) {
            handleEmptyPortfolio(accounts)
            return
        }

        if (holdings.isEmpty()) {
            handleCashOnlyPortfolio(cashItems, accounts)
            return
        }

        val currentSuccess = _uiState.value as? DashboardUiState.Success
        val isRefreshing = currentSuccess?.isRefreshing == true

        // Update accounts immediately if we have cached state (for cold start)
        if (currentSuccess != null && currentSuccess.accounts.isEmpty() && accounts.isNotEmpty()) {
            _uiState.value = currentSuccess.copy(accounts = accounts)
        }

        val symbols = holdings.map { it.symbol }.distinct()
        val existingStocks = currentSuccess?.stocks ?: emptyList()
        val newSymbols = symbols.filter { it !in existingStocks.map { s -> s.symbol }.toSet() }

        if (currentSuccess != null && newSymbols.isEmpty() && !isRefreshing) {
            handleIncrementalUpdate(holdings, cashItems, accounts, allAccounts, existingStocks, currentSuccess)
            return
        }

        handleFullRefresh(holdings, cashItems, accounts, allAccounts, symbols, currentSuccess, isRefreshing)
    }

    private suspend fun handleEmptyPortfolio(accounts: List<AccountWithCount>) {
        val showInKrw = getShowInKrwForCurrentAccount()
        val currentSuccess = _uiState.value as? DashboardUiState.Success
        // Empty portfolio = no sparkline, no returns (showing cached data would be misleading)
        _uiState.value = DashboardUiState.Success(
            stocks = emptyList(),
            cashItems = emptyList(),
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            periodReturns = emptyMap(),
            selectedPeriod = currentSuccess?.selectedPeriod ?: summaryPeriod,
            exchangeRate = currentExchangeRate,
            showInKrw = showInKrw,
            sparklinePeriod = sparklinePeriod,
            portfolioSparkline = emptyList(),
            portfolioStats = PortfolioStats(),
            sortOption = currentSortOption
        )
    }

    private suspend fun handleCashOnlyPortfolio(
        cashItems: List<CashItem>,
        accounts: List<AccountWithCount>
    ) {
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
        loadAllPeriodReturns(emptyList(), sortedCashItems)
    }

    private suspend fun handleIncrementalUpdate(
        holdings: List<HoldingEntity>,
        cashItems: List<CashItem>,
        accounts: List<AccountWithCount>,
        allAccounts: List<AccountEntity>,
        existingStocks: List<Stock>,
        currentSuccess: DashboardUiState.Success
    ) {
        val updatedStocks = mergeHoldingsWithExistingStocks(holdings, existingStocks, allAccounts)
        val sortedCashItems = sortCashItems(cashItems, currentSortOption)

        // Update in-memory cache
        if (selectedAccountId == ALL_ACCOUNTS_ID) {
            allStocksCache = updatedStocks
        } else {
            val cacheMap = allStocksCache.associateBy { it.symbol }.toMutableMap()
            updatedStocks.forEach { stock -> cacheMap[stock.symbol] = stock }
            allStocksCache = cacheMap.values.toList()
        }

        val accountsWithRebalance = calculateAccountsWithRebalanceStatus(accounts, currentSuccess.showInKrw)
        _uiState.value = currentSuccess.copy(
            stocks = updatedStocks,
            cashItems = sortedCashItems,
            accounts = accountsWithRebalance,
            selectedAccountId = selectedAccountId
        )
        if (selectedAccountId == ALL_ACCOUNTS_ID) {
            saveStateToCache(updatedStocks, sortedCashItems, accountsWithRebalance, currentExchangeRate)
        }
        loadBenchmarkData(currentSuccess.selectedPeriod)
        loadPortfolioSparkline(updatedStocks, sortedCashItems, currentSuccess.selectedPeriod)
        loadAllPeriodReturns(updatedStocks, sortedCashItems)
    }

    private suspend fun handleFullRefresh(
        holdings: List<HoldingEntity>,
        cashItems: List<CashItem>,
        accounts: List<AccountWithCount>,
        allAccounts: List<AccountEntity>,
        symbols: List<String>,
        currentSuccess: DashboardUiState.Success?,
        isRefreshing: Boolean
    ) {
        if (!isRefreshing && currentSuccess == null) {
            _uiState.value = DashboardUiState.Loading
        }

        val result = stockRepository.getQuotes(symbols)

        result.fold(
            onSuccess = { quotes ->
                val priceHistoryMap = stockRepository.getPriceHistory(symbols, sparklinePeriod.range)

                val stocks = if (selectedAccountId == ALL_ACCOUNTS_ID) {
                    aggregateHoldings(holdings, quotes, allAccounts, priceHistoryMap)
                } else {
                    holdings.map { holding ->
                        val quote = quotes.find { it.symbol == holding.symbol }
                        StockMapper.createStock(holding, quote, priceHistoryMap[holding.symbol])
                    }
                }
                val sortedStocks = sortStocks(stocks, currentSortOption)
                val sortedCashItems = sortCashItems(cashItems, currentSortOption)

                // Update in-memory cache with fresh stock data
                if (selectedAccountId == ALL_ACCOUNTS_ID) {
                    // Replace entire cache when viewing all accounts
                    allStocksCache = stocks
                } else {
                    // Merge when viewing specific account (preserves stocks from other accounts)
                    val cacheMap = allStocksCache.associateBy { it.symbol }.toMutableMap()
                    stocks.forEach { stock -> cacheMap[stock.symbol] = stock }
                    allStocksCache = cacheMap.values.toList()
                }

                // Skip UI update if data is same as current state (avoids blink)
                if (currentSuccess != null && stocksAreEqual(sortedStocks, currentSuccess.stocks)) {
                    // Just mark refresh as done, but update accounts with rebalance status
                    val showInKrw = currentSuccess.showInKrw
                    val accountsWithRebalance = calculateAccountsWithRebalanceStatus(accounts, showInKrw)
                    updateSuccessState { it.copy(isRefreshing = false, accounts = accountsWithRebalance) }
                    if (selectedAccountId == ALL_ACCOUNTS_ID) {
                        saveStateToCache(sortedStocks, sortedCashItems, accountsWithRebalance, currentExchangeRate)
                    }
                    // Still need to load sparkline/benchmark if timestamps are missing
                    val selectedPeriod = currentSuccess.selectedPeriod
                    if (currentSuccess.portfolioSparklineTimestamps.size < 2) {
                        loadPortfolioSparkline(stocks, sortedCashItems, selectedPeriod)
                    }
                    if (currentSuccess.benchmarkSparklines.isEmpty()) {
                        loadBenchmarkData(selectedPeriod)
                    }
                    return@fold
                }

                val previousReturns = currentSuccess?.periodReturns ?: emptyMap()
                val selectedPeriod = currentSuccess?.selectedPeriod ?: summaryPeriod
                val showInKrw = getShowInKrwForCurrentAccount()
                val accountsWithRebalance = calculateAccountsWithRebalanceStatus(accounts, showInKrw)
                _uiState.value = DashboardUiState.Success(
                    stocks = sortedStocks,
                    cashItems = sortedCashItems,
                    accounts = accountsWithRebalance,
                    selectedAccountId = selectedAccountId,
                    periodReturns = previousReturns,
                    benchmarkReturns = currentSuccess?.benchmarkReturns ?: emptyMap(),
                    selectedPeriod = selectedPeriod,
                    isRefreshing = false,
                    exchangeRate = currentExchangeRate,
                    showInKrw = showInKrw,
                    sparklinePeriod = sparklinePeriod,
                    portfolioSparkline = currentSuccess?.portfolioSparkline ?: emptyList(),
                    portfolioSparklineTimestamps = currentSuccess?.portfolioSparklineTimestamps ?: emptyList(),
                    benchmarkSparklines = currentSuccess?.benchmarkSparklines ?: emptyMap(),
                    portfolioStats = currentSuccess?.portfolioStats ?: PortfolioStats(),
                    sortOption = currentSortOption
                )
                if (selectedAccountId == ALL_ACCOUNTS_ID) {
                    saveStateToCache(sortedStocks, sortedCashItems, accountsWithRebalance, currentExchangeRate)
                }
                loadAllPeriodReturns(stocks, sortedCashItems)
                loadBenchmarkData(selectedPeriod)
                loadPortfolioSparkline(stocks, sortedCashItems, selectedPeriod)
            },
            onFailure = { exception ->
                _uiState.value = DashboardUiState.Error(
                    exception.message ?: ErrorMessages.LOAD_PRICES_FAILED
                )
            }
        )
    }

    /**
     * Checks if two stock lists have the same essential data (avoids unnecessary re-renders).
     */
    private fun stocksAreEqual(stocks1: List<Stock>, stocks2: List<Stock>): Boolean {
        if (stocks1.size != stocks2.size) return false
        val map1 = stocks1.associateBy { it.symbol }
        val map2 = stocks2.associateBy { it.symbol }
        if (map1.keys != map2.keys) return false
        return map1.all { (symbol, stock1) ->
            val stock2 = map2[symbol] ?: return false
            stock1.currentPrice == stock2.currentPrice &&
                stock1.quantity == stock2.quantity &&
                stock1.averagePrice == stock2.averagePrice &&
                stock1.dayChange == stock2.dayChange
        }
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
        return RebalanceCalculator.buildRebalanceItems(
            holdings, state.stocks, state.showInKrw, currentExchangeRate
        )
    }

    fun saveTargetPercentages(percentages: Map<Long, Int>) {
        viewModelScope.launch {
            percentages.forEach { (holdingId, percentage) ->
                holdingsRepository.updateTargetPercentage(holdingId, percentage)
            }
        }
    }

    fun resetTargetPercentages() {
        viewModelScope.launch {
            if (selectedAccountId == ALL_ACCOUNTS_ID) return@launch
            val holdings = holdingsRepository.getHoldingsByAccountSync(selectedAccountId)
            holdings.forEach { holding ->
                holdingsRepository.updateTargetPercentage(holding.id, null)
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

    /**
     * Calculates rebalance status for each account.
     * An account needs rebalancing if any holding has diffAmount >= currentPrice.
     */
    private suspend fun calculateAccountsWithRebalanceStatus(
        accounts: List<AccountWithCount>,
        showInKrw: Boolean
    ): List<AccountWithCount> {
        // Use allStocksCache which contains stocks from all accounts
        val stockMap = allStocksCache.associateBy { it.symbol }

        return accounts.map { accountWithCount ->
            val needsRebalance = accountNeedsRebalance(
                accountWithCount.account.id, stockMap, showInKrw
            )
            accountWithCount.copy(needsRebalance = needsRebalance)
        }
    }

    /**
     * Checks if an account needs rebalancing.
     * Returns true if any holding has diffAmount >= currentPrice.
     */
    private suspend fun accountNeedsRebalance(
        accountId: Long,
        stockMap: Map<String, Stock>,
        showInKrw: Boolean
    ): Boolean {
        val holdings = holdingsRepository.getHoldingsByAccountSync(accountId)
        return RebalanceCalculator.accountNeedsRebalance(
            holdings, stockMap, showInKrw, currentExchangeRate
        )
    }
}
