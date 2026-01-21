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
import kotlin.math.pow
import kotlin.math.sqrt
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.presentation.util.CurrencyConverter
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var selectedAccountId: Long = ALL_ACCOUNTS_ID
    private var holdingsJob: Job? = null
    private var currentExchangeRate: Double = KRW_TO_USD_RATE

    private var allAccountsCurrencyKrw: Boolean
        get() = sharedPreferences.getBoolean(PREF_ALL_ACCOUNTS_CURRENCY_KRW, false)
        set(value) = sharedPreferences.edit().putBoolean(PREF_ALL_ACCOUNTS_CURRENCY_KRW, value).apply()

    companion object {
        private const val PREF_ALL_ACCOUNTS_CURRENCY_KRW = "all_accounts_currency_krw"
        private const val PREF_CACHED_STOCKS = "cached_stocks"
        private const val PREF_CACHED_EXCHANGE_RATE = "cached_exchange_rate"
        private const val PREF_SPARKLINE_PERIOD = "sparkline_period"
        private const val PREF_SUMMARY_PERIOD = "summary_period"
    }

    private var sparklinePeriod: TimePeriod
        get() {
            val ordinal = sharedPreferences.getInt(PREF_SPARKLINE_PERIOD, TimePeriod.ONE_YEAR.ordinal)
            return TimePeriod.entries.getOrElse(ordinal) { TimePeriod.ONE_YEAR }
        }
        set(value) = sharedPreferences.edit().putInt(PREF_SPARKLINE_PERIOD, value.ordinal).apply()

    private var summaryPeriod: TimePeriod
        get() {
            val ordinal = sharedPreferences.getInt(PREF_SUMMARY_PERIOD, TimePeriod.ONE_YEAR.ordinal)
            return TimePeriod.entries.getOrElse(ordinal) { TimePeriod.ONE_YEAR }
        }
        set(value) = sharedPreferences.edit().putInt(PREF_SUMMARY_PERIOD, value.ordinal).apply()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        viewModelScope.launch {
            // Load cached state immediately for fast startup
            loadCachedState()
            // Ensure default account exists (atomic operation to prevent race condition)
            accountRepository.getOrCreateDefaultAccount(DEFAULT_ACCOUNT_NAME)
            // Fetch exchange rate
            stockRepository.getExchangeRate("USD", "KRW").onSuccess { rate ->
                currentExchangeRate = rate
            }
            observeHoldings()
        }
    }

    private fun loadCachedState() {
        try {
            val cachedStocksJson = sharedPreferences.getString(PREF_CACHED_STOCKS, null)
            val cachedRate = sharedPreferences.getFloat(PREF_CACHED_EXCHANGE_RATE, KRW_TO_USD_RATE.toFloat()).toDouble()
            if (cachedStocksJson != null) {
                val stocks = json.decodeFromString<List<Stock>>(cachedStocksJson)
                currentExchangeRate = cachedRate
                _uiState.value = DashboardUiState.Success(
                    stocks = stocks,
                    selectedAccountId = selectedAccountId,
                    selectedPeriod = summaryPeriod,
                    exchangeRate = cachedRate,
                    showInKrw = allAccountsCurrencyKrw,
                    isRefreshing = true,
                    sparklinePeriod = sparklinePeriod
                )
            }
        } catch (_: Exception) {
            // Ignore cache errors, will load fresh data
        }
    }

    private fun saveStateToCache(stocks: List<Stock>, exchangeRate: Double) {
        try {
            val stocksJson = json.encodeToString(stocks)
            sharedPreferences.edit()
                .putString(PREF_CACHED_STOCKS, stocksJson)
                .putFloat(PREF_CACHED_EXCHANGE_RATE, exchangeRate.toFloat())
                .apply()
        } catch (_: Exception) {
            // Ignore cache errors
        }
    }

    fun selectAccount(accountId: Long) {
        selectedAccountId = accountId
        observeHoldings()
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
        observeHoldings()
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
        observeHoldings()
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
                updatePeriodReturn(period, 0.0)
                return@launch
            }

            // Fetch exchange rate history for currency-adjusted returns
            val exchangeRateData = stockRepository.getExchangeRateHistory("USD", "KRW", period.range)
            val startExchangeRate = exchangeRateData.prices.firstOrNull() ?: currentExchangeRate
            val endExchangeRate = exchangeRateData.prices.lastOrNull() ?: currentExchangeRate

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

            // Calculate weighted portfolio return with exchange rate consideration
            val weightedReturn = stocks.sumOf { stock ->
                val weight = stock.totalValueInUsd(currentExchangeRate) / totalPortfolioValue
                val stockReturn = returnsBySymbol[stock.symbol] ?: 0.0
                val adjustedReturn = adjustReturnForExchangeRate(
                    stockReturn, stock.currency, showInKrw, startExchangeRate, endExchangeRate
                )
                weight * adjustedReturn
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

                val stockDatePrices = buildStockDatePrices(stocksWithHistory, priceHistoryMap)
                val exchangeRateByDate = buildExchangeRateByDate(exchangeRateData)
                val allDates = stockDatePrices.values.flatMap { it.keys }.toSet().sorted()

                if (allDates.size < 2) {
                    updatePortfolioSparkline(emptyList())
                    return@launch
                }

                val filledStockPrices = forwardFillPrices(stocksWithHistory, stockDatePrices, allDates)
                val validDates = allDates.filter { date ->
                    stocksWithHistory.all { stock ->
                        filledStockPrices[stock.symbol]?.containsKey(date) == true
                    }
                }

                val portfolioValues = calculatePortfolioValuesForDates(
                    validDates, stocksWithHistory, filledStockPrices, exchangeRateByDate, showInKrw
                )

                if (portfolioValues.size < 2) {
                    updatePortfolioSparkline(emptyList(), PortfolioStats())
                    return@launch
                }

                val stats = calculatePortfolioStats(portfolioValues)
                updatePortfolioSparkline(normalizeValues(portfolioValues), stats)
            } catch (e: Exception) {
                updatePortfolioSparkline(emptyList(), PortfolioStats())
            }
        }
    }

    private fun calculatePortfolioStats(portfolioValues: List<Double>): PortfolioStats {
        if (portfolioValues.size < 2) return PortfolioStats()

        // Calculate daily returns as percentages
        val dailyReturns = portfolioValues.zipWithNext { a, b ->
            if (a > 0) ((b - a) / a) * 100 else 0.0
        }

        return PortfolioStats(
            maxDrawdown = calculateMDD(portfolioValues),
            volatility = calculateVolatility(dailyReturns),
            sharpeRatio = calculateSharpeRatio(dailyReturns),
            bestDay = dailyReturns.maxOrNull() ?: 0.0,
            worstDay = dailyReturns.minOrNull() ?: 0.0
        )
    }

    private fun calculateMDD(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        var maxDrawdown = 0.0
        var peak = values[0]

        for (value in values) {
            if (value > peak) peak = value
            if (peak > 0) {
                val drawdown = (peak - value) / peak * 100
                if (drawdown > maxDrawdown) maxDrawdown = drawdown
            }
        }
        return maxDrawdown
    }

    private fun calculateVolatility(dailyReturns: List<Double>): Double {
        if (dailyReturns.isEmpty()) return 0.0
        val mean = dailyReturns.average()
        val variance = dailyReturns.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }

    private fun calculateSharpeRatio(dailyReturns: List<Double>): Double {
        if (dailyReturns.isEmpty()) return 0.0
        val riskFreeRate = 0.02 // 2% annual risk-free rate
        val avgDailyReturn = dailyReturns.average()
        val dailyVolatility = calculateVolatility(dailyReturns)

        // Annualize: multiply returns by 252 trading days, volatility by sqrt(252)
        val annualizedReturn = avgDailyReturn * 252
        val annualizedVolatility = dailyVolatility * sqrt(252.0)

        return if (annualizedVolatility > 0) {
            (annualizedReturn - riskFreeRate) / annualizedVolatility
        } else {
            0.0
        }
    }

    private fun timestampToDate(timestamp: Long): String {
        // Convert Unix timestamp (seconds) to date string YYYY-MM-DD
        val instant = java.time.Instant.ofEpochSecond(timestamp)
        return java.time.LocalDate.ofInstant(instant, java.time.ZoneId.systemDefault()).toString()
    }

    private fun convertValue(value: Double, currency: String, showInKrw: Boolean, exchangeRate: Double): Double {
        return CurrencyConverter.convert(value, currency, showInKrw, exchangeRate)
    }

    /**
     * Builds a map of symbol to date-price mapping from price history data.
     */
    private fun buildStockDatePrices(
        stocks: List<Stock>,
        priceHistoryMap: Map<String, PriceHistoryData>
    ): Map<String, Map<String, Double>> {
        return stocks.associate { stock ->
            val data = priceHistoryMap[stock.symbol]!!
            val datePriceMap = if (data.timestamps.isNotEmpty() && data.timestamps.size == data.prices.size) {
                data.timestamps.zip(data.prices).associate { (ts, price) ->
                    timestampToDate(ts) to price
                }
            } else {
                data.prices.mapIndexed { index, price ->
                    "idx_$index" to price
                }.toMap()
            }
            stock.symbol to datePriceMap
        }
    }

    /**
     * Builds a map of date to exchange rate from price history data.
     */
    private fun buildExchangeRateByDate(exchangeRateData: PriceHistoryData): Map<String, Double> {
        return if (exchangeRateData.timestamps.isNotEmpty()) {
            exchangeRateData.timestamps.zip(exchangeRateData.prices).associate { (ts, rate) ->
                timestampToDate(ts) to rate
            }
        } else {
            emptyMap()
        }
    }

    /**
     * Forward-fills prices for each stock across all dates.
     * Missing dates use the last known price.
     */
    private fun forwardFillPrices(
        stocks: List<Stock>,
        stockDatePrices: Map<String, Map<String, Double>>,
        allDates: List<String>
    ): Map<String, Map<String, Double>> {
        return stocks.associate { stock ->
            val originalPrices = stockDatePrices[stock.symbol] ?: emptyMap()
            val filledPrices = mutableMapOf<String, Double>()
            var lastPrice: Double? = null

            for (date in allDates) {
                val price = originalPrices[date]
                if (price != null) {
                    lastPrice = price
                    filledPrices[date] = price
                } else if (lastPrice != null) {
                    filledPrices[date] = lastPrice
                }
            }
            stock.symbol to filledPrices
        }
    }

    /**
     * Calculates portfolio values for each valid date.
     */
    private fun calculatePortfolioValuesForDates(
        validDates: List<String>,
        stocks: List<Stock>,
        filledStockPrices: Map<String, Map<String, Double>>,
        exchangeRateByDate: Map<String, Double>,
        showInKrw: Boolean
    ): List<Double> {
        return validDates.mapNotNull { date ->
            var totalValue = 0.0
            val exchangeRate = (exchangeRateByDate[date] ?: currentExchangeRate)
                .takeIf { it > 0 } ?: currentExchangeRate

            for (stock in stocks) {
                val price = filledStockPrices[stock.symbol]?.get(date) ?: continue
                val value = price * stock.quantity
                totalValue += convertValue(value, stock.currency, showInKrw, exchangeRate)
            }
            if (totalValue > 0) totalValue else null
        }
    }

    /**
     * Adjusts stock return for exchange rate changes based on currency and display preference.
     *
     * When viewing USD stocks in KRW, the return needs to account for exchange rate changes.
     * When viewing KRW stocks in USD, the return needs to account for inverse exchange rate changes.
     */
    private fun adjustReturnForExchangeRate(
        stockReturn: Double,
        currency: String,
        showInKrw: Boolean,
        startExchangeRate: Double,
        endExchangeRate: Double
    ): Double {
        return when {
            showInKrw && currency == "USD" -> {
                // USD stock viewed in KRW: factor in exchange rate change
                val exchangeRateReturn = (endExchangeRate - startExchangeRate) / startExchangeRate * 100
                stockReturn + exchangeRateReturn + (stockReturn * exchangeRateReturn / 100)
            }
            !showInKrw && currency == "KRW" -> {
                // KRW stock viewed in USD: factor in inverse exchange rate change
                val exchangeRateReturn = (startExchangeRate - endExchangeRate) / endExchangeRate * 100
                stockReturn + exchangeRateReturn + (stockReturn * exchangeRateReturn / 100)
            }
            else -> stockReturn
        }
    }

    private fun normalizeValues(values: List<Double>): List<Double> {
        val startValue = values.firstOrNull() ?: 0.0
        return if (startValue > 0) {
            values.map { 100.0 * (it / startValue) }
        } else {
            values
        }
    }

    private fun updatePortfolioSparkline(sparkline: List<Double>, stats: PortfolioStats = PortfolioStats()) {
        val currentState = _uiState.value
        if (currentState is DashboardUiState.Success) {
            _uiState.value = currentState.copy(
                portfolioSparkline = sparkline,
                portfolioStats = stats
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
        val isRefreshing = currentState is DashboardUiState.Success && currentState.isRefreshing
        if (!isRefreshing) {
            _uiState.value = DashboardUiState.Loading
        }

        val symbols = holdings.map { it.symbol }.distinct()
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
                val previousReturns = if (currentState is DashboardUiState.Success) {
                    currentState.periodReturns
                } else {
                    emptyMap()
                }
                val selectedPeriod = if (currentState is DashboardUiState.Success) {
                    currentState.selectedPeriod
                } else {
                    summaryPeriod
                }
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
                    sparklinePeriod = sparklinePeriod
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
