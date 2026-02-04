package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences
import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.model.BenchmarkReturns
import com.portfolio.manager.domain.model.PeriodReturn
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.SortOption
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.CashRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.domain.service.PortfolioCache
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private lateinit var stockRepository: StockRepository
    private lateinit var holdingsRepository: HoldingsRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var cashRepository: CashRepository
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var portfolioCache: PortfolioCache
    private val testDispatcher = UnconfinedTestDispatcher()

    private val defaultAccount = AccountEntity(1, "Default", 1000L)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock android.util.Log
        mockkStatic(Log::class)
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0

        stockRepository = mockk()
        holdingsRepository = mockk()
        accountRepository = mockk()
        cashRepository = mockk()
        sharedPreferences = mockk()
        sharedPreferencesEditor = mockk()
        portfolioCache = PortfolioCache()

        // SharedPreferences mock
        every { sharedPreferences.getBoolean(any(), any()) } returns false
        every { sharedPreferences.getString(any(), any()) } returns null
        every { sharedPreferences.getFloat(any(), any()) } answers { secondArg() }
        every { sharedPreferences.getInt(any(), any()) } answers { secondArg() }
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putBoolean(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putString(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putFloat(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.putInt(any(), any()) } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.apply() } returns Unit

        // Default account setup
        coEvery { accountRepository.getOrCreateDefaultAccount(any()) } returns defaultAccount
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(defaultAccount))
        every { holdingsRepository.getHoldingsCountByAccountFlow() } returns flowOf(emptyMap())
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        // Default holdings by account sync (for rebalance calculation)
        coEvery { holdingsRepository.getHoldingsByAccountSync(any()) } returns emptyList()
        // Default cash repository mock
        every { cashRepository.getAllCashItems() } returns flowOf(emptyList())
        every { cashRepository.getCashItemsByAccount(any()) } returns flowOf(emptyList())
        // Default exchange rate mock
        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(1400.0)
        // Default exchange rate history mock
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns PriceHistoryData(
            prices = listOf(1400.0, 1400.0),
            timestamps = listOf(1000L, 2000L)
        )
        // Default price history mock (empty - graceful handling)
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns emptyMap<String, PriceHistoryData>()
        // Default period returns mock
        coEvery { stockRepository.getPeriodReturn(any(), any()) } answers {
            val symbol = firstArg<String>()
            val period = secondArg<TimePeriod>()
            Result.success(PeriodReturn(symbol, period, 1.5))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun `empty holdings - returns empty success`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        assertThat(viewModel.uiState.value).isInstanceOf(DashboardUiState.Success::class.java)
        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.stocks).isEmpty()
        assertThat(state.accounts).hasSize(1)
        assertThat(state.selectedAccountId).isEqualTo(ALL_ACCOUNTS_ID)
    }

    @Test
    fun `loadPrices - success - updates stocks with real prices`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        )
        val quotes = listOf(
            QuoteResult(
                symbol = "AAPL",
                shortName = "Apple Inc.",
                regularMarketPrice = 180.00,
                regularMarketChange = 5.00,
                regularMarketChangePercent = 2.86,
                currency = "USD"
            )
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        val state = viewModel.uiState.value as DashboardUiState.Success

        assertThat(state.stocks).hasSize(1)
        val appleStock = state.stocks.find { it.symbol == "AAPL" }
        assertThat(appleStock).isNotNull()
        assertThat(appleStock?.currentPrice).isEqualTo(180.00)
        assertThat(appleStock?.dayChange).isEqualTo(5.00)
        assertThat(appleStock?.dayChangePercent).isEqualTo(2.86)
    }

    @Test
    fun `loadPrices - failure - emits Error state`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.failure(IOException("Network error"))

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        assertThat(viewModel.uiState.value).isInstanceOf(DashboardUiState.Error::class.java)
        val errorState = viewModel.uiState.value as DashboardUiState.Error
        assertThat(errorState.message).contains("Network error")
    }

    @Test
    fun `refresh - reloads prices`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        )
        val initialQuotes = listOf(
            QuoteResult(symbol = "AAPL", regularMarketPrice = 175.00)
        )
        val refreshedQuotes = listOf(
            QuoteResult(symbol = "AAPL", regularMarketPrice = 180.00)
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returnsMany listOf(
            Result.success(initialQuotes),
            Result.success(refreshedQuotes)
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.refresh()

        val state = viewModel.uiState.value as DashboardUiState.Success
        val appleStock = state.stocks.find { it.symbol == "AAPL" }
        assertThat(appleStock?.currentPrice).isEqualTo(180.00)
    }

    @Test
    fun `loadPrices - uses longName first then shortName then symbol`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "005930.KS", "Samsung", 50, 72000.0, "KRW")
        )
        val quotes = listOf(
            QuoteResult(
                symbol = "005930.KS",
                shortName = "Samsung Electronics",
                longName = "Samsung Electronics Co., Ltd.",
                regularMarketPrice = 78500.0,
                regularMarketChange = 500.0,
                regularMarketChangePercent = 0.64,
                currency = "KRW"
            )
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        val state = viewModel.uiState.value as DashboardUiState.Success

        val samsungStock = state.stocks.find { it.symbol == "005930.KS" }
        assertThat(samsungStock).isNotNull()
        // Should use longName first
        assertThat(samsungStock?.name).isEqualTo("Samsung Electronics Co., Ltd.")
        assertThat(samsungStock?.currency).isEqualTo("KRW")
        assertThat(samsungStock?.currentPrice).isEqualTo(78500.0)
    }

    @Test
    fun `selectAccount - switches to different account`() = runTest {
        val account1 = AccountEntity(1, "Default", 1000L)
        val account2 = AccountEntity(2, "Trading", 2000L)
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account1, account2))
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        every { holdingsRepository.getHoldingsByAccount(2L) } returns flowOf(
            listOf(HoldingEntity(1, 2L, "TSLA", "Tesla", 5, 200.0, "USD"))
        )
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "TSLA", regularMarketPrice = 250.0))
        )
        coEvery { accountRepository.getAccountById(2L) } returns account2

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectAccount(2L)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.selectedAccountId).isEqualTo(2L)
        assertThat(state.stocks).hasSize(1)
        assertThat(state.stocks.first().symbol).isEqualTo("TSLA")
    }

    @Test
    fun `all accounts view - same symbol aggregated into single stock`() = runTest {
        val account1 = AccountEntity(1, "Default", 1000L)
        val account2 = AccountEntity(2, "Trading", 2000L)
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account1, account2))
        every { holdingsRepository.getHoldingsCountByAccountFlow() } returns flowOf(mapOf(1L to 1, 2L to 1))
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD"),
            HoldingEntity(2, 2L, "AAPL", "Apple Inc.", 5, 160.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", shortName = "Apple Inc.", regularMarketPrice = 180.0))
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        val state = viewModel.uiState.value as DashboardUiState.Success

        // Should show single aggregated stock, not 2 separate entries
        assertThat(state.stocks).hasSize(1)
        val appleStock = state.stocks.first()
        assertThat(appleStock.symbol).isEqualTo("AAPL")
        // Total quantity: 10 + 5 = 15
        assertThat(appleStock.quantity).isEqualTo(15)
        // Weighted average price: (10*150 + 5*160) / 15 = 2300/15 = 153.33...
        assertThat(appleStock.averagePrice).isWithin(0.01).of(153.33)
        // Should have account details for expansion
        assertThat(appleStock.accountDetails).hasSize(2)
    }

    @Test
    fun `all accounts view - different symbols shown separately`() = runTest {
        val account1 = AccountEntity(1, "Default", 1000L)
        val account2 = AccountEntity(2, "Trading", 2000L)
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account1, account2))
        every { holdingsRepository.getHoldingsCountByAccountFlow() } returns flowOf(mapOf(1L to 1, 2L to 1))
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD"),
            HoldingEntity(2, 2L, "TSLA", "Tesla", 5, 200.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(
                QuoteResult(symbol = "AAPL", shortName = "Apple Inc.", regularMarketPrice = 180.0),
                QuoteResult(symbol = "TSLA", shortName = "Tesla", regularMarketPrice = 250.0)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        val state = viewModel.uiState.value as DashboardUiState.Success

        assertThat(state.stocks).hasSize(2)
        assertThat(state.stocks.map { it.symbol }).containsExactly("AAPL", "TSLA")
    }

    @Test
    fun `single account view - no aggregation needed`() = runTest {
        val account1 = AccountEntity(1, "Default", 1000L)
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account1))
        every { holdingsRepository.getHoldingsCountByAccountFlow() } returns flowOf(mapOf(1L to 1))
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        every { holdingsRepository.getHoldingsByAccount(1L) } returns flowOf(
            listOf(HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD"))
        )
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", shortName = "Apple Inc.", regularMarketPrice = 180.0))
        )
        coEvery { accountRepository.getAccountById(1L) } returns account1

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectAccount(1L)
        val state = viewModel.uiState.value as DashboardUiState.Success

        assertThat(state.stocks).hasSize(1)
        val appleStock = state.stocks.first()
        assertThat(appleStock.accountDetails).isEmpty()
    }

    @Test
    fun `deleteHolding - calls repository delete`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        coEvery { holdingsRepository.deleteHolding(any()) } returns Unit

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.deleteHolding(1L)

        coVerify { holdingsRepository.deleteHolding(1L) }
    }

    @Test
    fun `selectPeriod - updates selected period and loads returns`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 180.0))
        )
        // Period return is now calculated from price history: (105.5 - 100) / 100 * 100 = 5.5%
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(listOf("AAPL"), "1mo") } returns mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(100.0, 105.5),
                timestamps = listOf(day1, day1 + 86400)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.ONE_MONTH)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.selectedPeriod).isEqualTo(TimePeriod.ONE_MONTH)
        assertThat(state.periodReturns[TimePeriod.ONE_MONTH]).isWithin(0.01).of(5.5)
    }

    @Test
    fun `selectPeriod - calculates weighted return for multiple stocks`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD"),
            HoldingEntity(2, 1L, "GOOGL", "Google", 5, 200.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(
                QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0),
                QuoteResult(symbol = "GOOGL", regularMarketPrice = 200.0)
            )
        )
        // AAPL: totalValue = 10 * 100 = 1000 USD
        // GOOGL: totalValue = 5 * 200 = 1000 USD
        // Total: 2000 USD
        // Weights: AAPL 50%, GOOGL 50%
        // Period return is calculated from portfolio values
        // Start: 10*100 + 5*200 = 2000, End: 10*110 + 5*240 = 2300 -> 15% return
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(any(), "5d") } returns mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(100.0, 110.0),
                timestamps = listOf(day1, day1 + 86400)
            ),
            "GOOGL" to PriceHistoryData(
                prices = listOf(200.0, 240.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.ONE_WEEK)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // Portfolio: Start=2000, End=2300 -> (2300-2000)/2000*100 = 15%
        assertThat(state.periodReturns[TimePeriod.ONE_WEEK]).isWithin(0.01).of(15.0)
    }

    @Test
    fun `selectPeriod - empty stocks - does not load period returns`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.ONE_YEAR)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.periodReturns).isEmpty()
    }

    @Test
    fun `selectPeriod - failed period return - uses zero for that stock`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0))
        )
        coEvery { stockRepository.getPeriodReturn("AAPL", TimePeriod.SIX_MONTHS) } returns
            Result.failure(Exception("No data"))

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.SIX_MONTHS)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.periodReturns[TimePeriod.SIX_MONTHS]).isEqualTo(0.0)
    }

    @Test
    fun `init - fetches exchange rate on startup`() = runTest {
        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(1350.0)

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        coVerify { stockRepository.getExchangeRate("USD", "KRW") }
        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.exchangeRate).isEqualTo(1350.0)
    }

    @Test
    fun `init - exchange rate failure - uses default rate`() = runTest {
        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.failure(IOException("Network error"))

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.exchangeRate).isEqualTo(1400.0) // Default KRW_TO_USD_RATE
    }

    @Test
    fun `success state includes exchange rate`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 180.0))
        )
        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(1300.0)

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.exchangeRate).isEqualTo(1300.0)
    }

    @Test
    fun `toggleCurrency - updates showInKrw state for all accounts`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        val initialState = viewModel.uiState.value as DashboardUiState.Success
        assertThat(initialState.showInKrw).isFalse()

        viewModel.toggleCurrency()

        val toggledState = viewModel.uiState.value as DashboardUiState.Success
        assertThat(toggledState.showInKrw).isTrue()
    }

    @Test
    fun `toggleCurrency - updates account preferred currency in db`() = runTest {
        val account = AccountEntity(1, "Default", 1000L, preferredCurrency = "USD")
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account))
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        every { holdingsRepository.getHoldingsByAccount(1L) } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountById(1L) } returns account
        coEvery { accountRepository.updatePreferredCurrency(any(), any()) } returns Unit

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectAccount(1L)
        viewModel.toggleCurrency()

        coVerify { accountRepository.updatePreferredCurrency(1L, "KRW") }
    }

    @Test
    fun `selectAccount - loads currency preference from account`() = runTest {
        val account = AccountEntity(1, "Default", 1000L, preferredCurrency = "KRW")
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account))
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        every { holdingsRepository.getHoldingsByAccount(1L) } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountById(1L) } returns account

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectAccount(1L)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.showInKrw).isTrue()
    }

    @Test
    fun `selectAccount - all accounts uses SharedPreferences currency state`() = runTest {
        // Track the saved preference value
        var savedPref = false
        every { sharedPreferences.getBoolean(any(), any()) } answers { savedPref }
        every { sharedPreferencesEditor.putBoolean(any(), any()) } answers {
            savedPref = secondArg()
            sharedPreferencesEditor
        }

        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        // Toggle to KRW while in all accounts view
        viewModel.toggleCurrency()
        var state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.showInKrw).isTrue()

        // Switch to an account and back to all
        coEvery { accountRepository.getAccountById(1L) } returns defaultAccount
        every { holdingsRepository.getHoldingsByAccount(1L) } returns flowOf(emptyList())
        viewModel.selectAccount(1L)
        viewModel.selectAccount(ALL_ACCOUNTS_ID)

        // Should remember KRW preference for all accounts (via SharedPreferences)
        state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.showInKrw).isTrue()
    }

    @Test
    fun `selectSparklinePeriod - updates sparkline period and reloads data`() = runTest {
        // Track the saved sparkline period
        var savedSparklinePeriod = TimePeriod.ONE_YEAR.ordinal
        every { sharedPreferences.getInt("stock_sparkline_period", any()) } answers { savedSparklinePeriod }
        every { sharedPreferencesEditor.putInt("stock_sparkline_period", any()) } answers {
            savedSparklinePeriod = secondArg()
            sharedPreferencesEditor
        }

        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 180.0))
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        // Initial state should have default period (ONE_YEAR)
        var state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.sparklinePeriod).isEqualTo(TimePeriod.ONE_YEAR)

        // Change sparkline period
        viewModel.selectSparklinePeriod(TimePeriod.ONE_MONTH)

        state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.sparklinePeriod).isEqualTo(TimePeriod.ONE_MONTH)
    }

    @Test
    fun `selectSparklinePeriod - persists preference`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectSparklinePeriod(TimePeriod.ONE_WEEK)

        io.mockk.verify { sharedPreferencesEditor.putInt(any(), TimePeriod.ONE_WEEK.ordinal) }
    }

    @Test
    fun `portfolioStats - calculated from portfolio values`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0))
        )
        // Price history with timestamps for sparkline calculation (each timestamp is a different day)
        // 1704067200 = Jan 1, 2024, 86400 = 1 day in seconds
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(100.0, 105.0, 102.0, 110.0, 108.0),
                timestamps = listOf(day1, day1 + 86400, day1 + 172800, day1 + 259200, day1 + 345600)
            )
        )
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns PriceHistoryData(
            prices = listOf(1400.0, 1400.0, 1400.0, 1400.0, 1400.0),
            timestamps = listOf(day1, day1 + 86400, day1 + 172800, day1 + 259200, day1 + 345600)
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.ONE_MONTH)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // Stats should be calculated (non-zero)
        assertThat(state.portfolioStats.maxDrawdown).isGreaterThan(0.0)
        assertThat(state.portfolioStats.volatility).isGreaterThan(0.0)
    }

    @Test
    fun `portfolioStats - empty when no price history`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0))
        )
        // Empty price history
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns emptyMap()

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.ONE_MONTH)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // Stats should be default (zeros)
        assertThat(state.portfolioStats).isEqualTo(PortfolioStats())
    }

    @Test
    fun `portfolioStats - MDD calculation - peak to trough`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0))
        )
        // Price goes: 100 -> 120 (peak) -> 96 (20% drawdown from peak)
        // 1704067200 = Jan 1, 2024, 86400 = 1 day in seconds
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(100.0, 120.0, 96.0),
                timestamps = listOf(day1, day1 + 86400, day1 + 172800)
            )
        )
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns PriceHistoryData(
            prices = listOf(1400.0, 1400.0, 1400.0),
            timestamps = listOf(day1, day1 + 86400, day1 + 172800)
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.ONE_MONTH)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // MDD should be 20% (from 120 to 96)
        assertThat(state.portfolioStats.maxDrawdown).isWithin(0.1).of(20.0)
    }

    @Test
    fun `portfolioStats - best and worst day calculated`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0))
        )
        // Price: 100 -> 110 (+10%) -> 99 (-10%)
        // 1704067200 = Jan 1, 2024, 86400 = 1 day in seconds
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(100.0, 110.0, 99.0),
                timestamps = listOf(day1, day1 + 86400, day1 + 172800)
            )
        )
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns PriceHistoryData(
            prices = listOf(1400.0, 1400.0, 1400.0),
            timestamps = listOf(day1, day1 + 86400, day1 + 172800)
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.ONE_MONTH)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // Best day: +10%
        assertThat(state.portfolioStats.bestDay).isWithin(0.1).of(10.0)
        // Worst day: -10% (from 110 to 99)
        assertThat(state.portfolioStats.worstDay).isWithin(0.1).of(-10.0)
    }

    @Test
    fun `selectPeriod - fetches benchmark returns in parallel`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0))
        )
        // Benchmark returns are now calculated from price history
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(listOf("AAPL"), "1mo") } returns mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(100.0, 105.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )
        // S&P 500: (103 - 100) / 100 * 100 = 3%
        coEvery { stockRepository.getPriceHistory(listOf("^GSPC"), "1mo") } returns mapOf(
            "^GSPC" to PriceHistoryData(
                prices = listOf(100.0, 103.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )
        // KOSPI: (102 - 100) / 100 * 100 = 2%
        coEvery { stockRepository.getPriceHistory(listOf("^KS11"), "1mo") } returns mapOf(
            "^KS11" to PriceHistoryData(
                prices = listOf(100.0, 102.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.ONE_MONTH)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.benchmarkReturns[TimePeriod.ONE_MONTH]?.sp500).isWithin(0.01).of(3.0)
        assertThat(state.benchmarkReturns[TimePeriod.ONE_MONTH]?.kospi).isWithin(0.01).of(2.0)
    }

    @Test
    fun `selectPeriod - benchmark fetch failure - returns null for failed benchmark`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0))
        )
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(listOf("AAPL"), "1y") } returns mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(100.0, 110.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )
        // S&P 500 returns empty (simulates failure/insufficient data)
        coEvery { stockRepository.getPriceHistory(listOf("^GSPC"), "1y") } returns mapOf(
            "^GSPC" to PriceHistoryData(prices = emptyList(), timestamps = emptyList())
        )
        // KOSPI: (105 - 100) / 100 * 100 = 5%
        coEvery { stockRepository.getPriceHistory(listOf("^KS11"), "1y") } returns mapOf(
            "^KS11" to PriceHistoryData(
                prices = listOf(100.0, 105.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.ONE_YEAR)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.benchmarkReturns[TimePeriod.ONE_YEAR]?.sp500).isNull()
        assertThat(state.benchmarkReturns[TimePeriod.ONE_YEAR]?.kospi).isWithin(0.01).of(5.0)
    }

    @Test
    fun `selectPeriod - both benchmarks fail - returns null for both`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0))
        )
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(listOf("AAPL"), "1y") } returns mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(100.0, 110.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )
        // Both benchmarks return empty (simulates failure/insufficient data)
        coEvery { stockRepository.getPriceHistory(listOf("^GSPC"), "1y") } returns mapOf(
            "^GSPC" to PriceHistoryData(prices = emptyList(), timestamps = emptyList())
        )
        coEvery { stockRepository.getPriceHistory(listOf("^KS11"), "1y") } returns mapOf(
            "^KS11" to PriceHistoryData(prices = emptyList(), timestamps = emptyList())
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.ONE_YEAR)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.benchmarkReturns[TimePeriod.ONE_YEAR]?.sp500).isNull()
        assertThat(state.benchmarkReturns[TimePeriod.ONE_YEAR]?.kospi).isNull()
    }

    @Test
    fun `selectPeriod - currency toggle KRW - adjusts S&P 500 for exchange rate`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0))
        )
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(listOf("AAPL"), "1mo") } returns mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(100.0, 110.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )
        // S&P 500: (110 - 100) / 100 * 100 = 10%
        coEvery { stockRepository.getPriceHistory(listOf("^GSPC"), "1mo") } returns mapOf(
            "^GSPC" to PriceHistoryData(
                prices = listOf(100.0, 110.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )
        // KOSPI: (105 - 100) / 100 * 100 = 5%
        coEvery { stockRepository.getPriceHistory(listOf("^KS11"), "1mo") } returns mapOf(
            "^KS11" to PriceHistoryData(
                prices = listOf(100.0, 105.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )
        // Exchange rate went from 1300 to 1400 = ~7.7% increase
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns PriceHistoryData(
            prices = listOf(1300.0, 1400.0),
            timestamps = listOf(day1, day1 + 86400)
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.toggleCurrency() // Switch to KRW
        viewModel.selectPeriod(TimePeriod.ONE_MONTH)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // S&P 500 return in KRW should include exchange rate appreciation
        // ~10% + ~7.7% + (10% * 7.7% / 100) = ~18.5%
        assertThat(state.benchmarkReturns[TimePeriod.ONE_MONTH]?.sp500).isGreaterThan(17.0)
        // KOSPI should remain unchanged (already in KRW)
        assertThat(state.benchmarkReturns[TimePeriod.ONE_MONTH]?.kospi).isWithin(0.01).of(5.0)
    }

    @Test
    fun `selectPeriod - USD view - adjusts KOSPI for exchange rate`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "005930.KS", "Samsung", 10, 70000.0, "KRW")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "005930.KS", regularMarketPrice = 70000.0))
        )
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(listOf("005930.KS"), "1mo") } returns mapOf(
            "005930.KS" to PriceHistoryData(
                prices = listOf(70000.0, 77000.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )
        // S&P 500: (105 - 100) / 100 * 100 = 5%
        coEvery { stockRepository.getPriceHistory(listOf("^GSPC"), "1mo") } returns mapOf(
            "^GSPC" to PriceHistoryData(
                prices = listOf(100.0, 105.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )
        // KOSPI: (110 - 100) / 100 * 100 = 10%
        coEvery { stockRepository.getPriceHistory(listOf("^KS11"), "1mo") } returns mapOf(
            "^KS11" to PriceHistoryData(
                prices = listOf(100.0, 110.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )
        // Exchange rate went from 1300 to 1400 (KRW weakened)
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns PriceHistoryData(
            prices = listOf(1300.0, 1400.0),
            timestamps = listOf(day1, day1 + 86400)
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        // Default is USD view, so KOSPI should be adjusted
        viewModel.selectPeriod(TimePeriod.ONE_MONTH)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // S&P 500 should remain unchanged in USD view
        assertThat(state.benchmarkReturns[TimePeriod.ONE_MONTH]?.sp500).isWithin(0.01).of(5.0)
        // KOSPI should be adjusted for exchange rate (inverse effect)
        // KOSPI gained 10%, but KRW weakened ~7.7%, so USD value is less
        assertThat(state.benchmarkReturns[TimePeriod.ONE_MONTH]?.kospi).isLessThan(10.0)
    }

    @Test
    fun `empty holdings - benchmarkReturns not populated`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectPeriod(TimePeriod.ONE_YEAR)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.benchmarkReturns).isEmpty()
    }

    @Test
    fun `selectSortOption - updates sort option in state`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD"),
            HoldingEntity(2, 1L, "GOOGL", "Google", 5, 200.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(
                QuoteResult(symbol = "AAPL", shortName = "Apple Inc.", regularMarketPrice = 180.0),
                QuoteResult(symbol = "GOOGL", shortName = "Google", regularMarketPrice = 250.0)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        // Default should be WEIGHT
        var state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.sortOption).isEqualTo(SortOption.WEIGHT)

        // Change to NAME
        viewModel.selectSortOption(SortOption.NAME)

        state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.sortOption).isEqualTo(SortOption.NAME)
    }

    @Test
    fun `selectSortOption - persists preference`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectSortOption(SortOption.SYMBOL)

        io.mockk.verify { sharedPreferencesEditor.putInt("sort_option", SortOption.SYMBOL.ordinal) }
    }

    @Test
    fun `selectSortOption - WEIGHT - sorts by total value descending`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 10, 100.0, "USD"),  // Value: 1000
            HoldingEntity(2, 1L, "GOOGL", "Google", 5, 100.0, "USD") // Value: 1500
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(
                QuoteResult(symbol = "AAPL", shortName = "Apple", regularMarketPrice = 100.0),
                QuoteResult(symbol = "GOOGL", shortName = "Google", regularMarketPrice = 300.0)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectSortOption(SortOption.WEIGHT)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // GOOGL (1500) should come before AAPL (1000)
        assertThat(state.stocks.map { it.symbol }).containsExactly("GOOGL", "AAPL").inOrder()
    }

    @Test
    fun `selectSortOption - NAME - sorts alphabetically ascending`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "GOOGL", "Google", 5, 100.0, "USD"),
            HoldingEntity(2, 1L, "AAPL", "Apple", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(
                QuoteResult(symbol = "GOOGL", shortName = "Google", regularMarketPrice = 100.0),
                QuoteResult(symbol = "AAPL", shortName = "Apple", regularMarketPrice = 100.0)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectSortOption(SortOption.NAME)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // Apple should come before Google
        assertThat(state.stocks.map { it.name }).containsExactly("Apple", "Google").inOrder()
    }

    @Test
    fun `selectSortOption - SYMBOL - sorts alphabetically ascending`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "TSLA", "Tesla", 5, 100.0, "USD"),
            HoldingEntity(2, 1L, "AAPL", "Apple", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(
                QuoteResult(symbol = "TSLA", shortName = "Tesla", regularMarketPrice = 100.0),
                QuoteResult(symbol = "AAPL", shortName = "Apple", regularMarketPrice = 100.0)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectSortOption(SortOption.SYMBOL)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // AAPL should come before TSLA
        assertThat(state.stocks.map { it.symbol }).containsExactly("AAPL", "TSLA").inOrder()
    }

    @Test
    fun `selectSortOption - GAIN_LOSS_PERCENT - sorts by gain loss percent descending`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 10, 100.0, "USD"),  // 50% gain
            HoldingEntity(2, 1L, "GOOGL", "Google", 5, 100.0, "USD") // 100% gain
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(
                QuoteResult(symbol = "AAPL", shortName = "Apple", regularMarketPrice = 150.0),
                QuoteResult(symbol = "GOOGL", shortName = "Google", regularMarketPrice = 200.0)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectSortOption(SortOption.GAIN_LOSS_PERCENT)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // GOOGL (100% gain) should come before AAPL (50% gain)
        assertThat(state.stocks.map { it.symbol }).containsExactly("GOOGL", "AAPL").inOrder()
    }

    @Test
    fun `selectSortOption - DAY_CHANGE_PERCENT - sorts by day change percent descending`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 10, 100.0, "USD"),
            HoldingEntity(2, 1L, "GOOGL", "Google", 5, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(
                QuoteResult(symbol = "AAPL", shortName = "Apple", regularMarketPrice = 100.0, regularMarketChangePercent = 2.0),
                QuoteResult(symbol = "GOOGL", shortName = "Google", regularMarketPrice = 100.0, regularMarketChangePercent = 5.0)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectSortOption(SortOption.DAY_CHANGE_PERCENT)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // GOOGL (5% day change) should come before AAPL (2% day change)
        assertThat(state.stocks.map { it.symbol }).containsExactly("GOOGL", "AAPL").inOrder()
    }

    @Test
    fun `selectSortOption - DAY_CHANGE_PERCENT - zero values sorted last`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 10, 100.0, "USD"),
            HoldingEntity(2, 1L, "GOOGL", "Google", 5, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(
                QuoteResult(symbol = "AAPL", shortName = "Apple", regularMarketPrice = 100.0, regularMarketChangePercent = 0.0),
                QuoteResult(symbol = "GOOGL", shortName = "Google", regularMarketPrice = 100.0, regularMarketChangePercent = 1.0)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectSortOption(SortOption.DAY_CHANGE_PERCENT)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // GOOGL (1%) should come before AAPL (0%)
        assertThat(state.stocks.map { it.symbol }).containsExactly("GOOGL", "AAPL").inOrder()
    }

    @Test
    fun `init - loads persisted sort option`() = runTest {
        every { sharedPreferences.getInt("sort_option", any()) } returns SortOption.SYMBOL.ordinal
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.sortOption).isEqualTo(SortOption.SYMBOL)
    }

    @Test
    fun `init - invalid persisted sort option - uses default WEIGHT`() = runTest {
        every { sharedPreferences.getInt("sort_option", any()) } returns 999 // Invalid ordinal
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.sortOption).isEqualTo(SortOption.WEIGHT)
    }

    @Test
    fun `canShowRebalance - all accounts view - returns false`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 180.0))
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        assertThat(viewModel.canShowRebalance()).isFalse()
    }

    @Test
    fun `canShowRebalance - single account with stocks - returns true`() = runTest {
        val account = AccountEntity(1, "Default", 1000L)
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account))
        every { holdingsRepository.getHoldingsByAccount(1L) } returns flowOf(
            listOf(HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD"))
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 180.0))
        )
        coEvery { accountRepository.getAccountById(1L) } returns account

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectAccount(1L)

        assertThat(viewModel.canShowRebalance()).isTrue()
    }

    @Test
    fun `canShowRebalance - single account with empty stocks - returns false`() = runTest {
        val account = AccountEntity(1, "Default", 1000L)
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account))
        every { holdingsRepository.getHoldingsByAccount(1L) } returns flowOf(emptyList())
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountById(1L) } returns account

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectAccount(1L)

        assertThat(viewModel.canShowRebalance()).isFalse()
    }

    @Test
    fun `getRebalanceItems - all accounts view - returns empty`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 180.0))
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        val items = viewModel.getRebalanceItems()
        assertThat(items).isEmpty()
    }

    @Test
    fun `getRebalanceItems - single account - returns items with values`() = runTest {
        val account = AccountEntity(1, "Default", 1000L)
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account))
        every { holdingsRepository.getHoldingsByAccount(1L) } returns flowOf(
            listOf(
                HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD", targetPercentage = 60),
                HoldingEntity(2, 1L, "GOOGL", "Google", 5, 200.0, "USD", targetPercentage = 40)
            )
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(
                QuoteResult(symbol = "AAPL", shortName = "Apple Inc.", regularMarketPrice = 100.0),
                QuoteResult(symbol = "GOOGL", shortName = "Google", regularMarketPrice = 200.0)
            )
        )
        coEvery { accountRepository.getAccountById(1L) } returns account
        coEvery { holdingsRepository.getHoldingsByAccountSync(1L) } returns listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD", targetPercentage = 60),
            HoldingEntity(2, 1L, "GOOGL", "Google", 5, 200.0, "USD", targetPercentage = 40)
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectAccount(1L)

        val items = viewModel.getRebalanceItems()
        assertThat(items).hasSize(2)
        assertThat(items.find { it.symbol == "AAPL" }?.currentPercentage).isEqualTo(60)
        assertThat(items.find { it.symbol == "GOOGL" }?.currentPercentage).isEqualTo(40)
    }

    @Test
    fun `saveTargetPercentages - calls repository for each percentage`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        coEvery { holdingsRepository.updateTargetPercentage(any(), any()) } returns Unit

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        val percentages = mapOf(1L to 60, 2L to 40)
        viewModel.saveTargetPercentages(percentages)

        coVerify { holdingsRepository.updateTargetPercentage(1L, 60) }
        coVerify { holdingsRepository.updateTargetPercentage(2L, 40) }
    }

    @Test
    fun `resetTargetPercentages - clears target percentages for specific account`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD", 60),
            HoldingEntity(2, 1L, "GOOGL", "Alphabet Inc.", 5, 200.0, "USD", 40)
        )
        every { holdingsRepository.getHoldingsByAccount(1L) } returns flowOf(holdings)
        coEvery { holdingsRepository.getHoldingsByAccountSync(1L) } returns holdings
        coEvery { holdingsRepository.updateTargetPercentage(any(), any()) } returns Unit
        coEvery { accountRepository.getAccountById(1L) } returns AccountEntity(id = 1L, name = "Test", orderIndex = 0, preferredCurrency = "USD")
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(
                QuoteResult(symbol = "AAPL", regularMarketPrice = 150.0),
                QuoteResult(symbol = "GOOGL", regularMarketPrice = 250.0)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectAccount(1L)

        viewModel.resetTargetPercentages()

        coVerify { holdingsRepository.updateTargetPercentage(1L, null) }
        coVerify { holdingsRepository.updateTargetPercentage(2L, null) }
    }

    @Test
    fun `resetTargetPercentages - does nothing for all accounts view`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        // Default is ALL_ACCOUNTS_ID

        viewModel.resetTargetPercentages()

        coVerify(exactly = 0) { holdingsRepository.getHoldingsByAccountSync(any()) }
        coVerify(exactly = 0) { holdingsRepository.updateTargetPercentage(any(), any()) }
    }

    @Test
    fun `getStocksValue - returns sum of stock values`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 150.0))
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        // Total value: 10 * 150 = 1500
        assertThat(viewModel.getStocksValue()).isEqualTo(1500.0)
    }

    @Test
    fun `isShowingInKrw - returns currency preference`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        assertThat(viewModel.isShowingInKrw()).isFalse()

        viewModel.toggleCurrency()
        assertThat(viewModel.isShowingInKrw()).isTrue()
    }

    @Test
    fun `cold start - empty portfolio clears period returns`() = runTest {
        // Test that empty portfolio shows empty returns (not stale cached data)
        val cachedStocksJson = """[{"id":1,"symbol":"AAPL","name":"Apple","quantity":10,"averagePrice":100.0,"currentPrice":150.0,"currency":"USD","accountDetails":[],"priceHistory":[],"priceHistoryTimestamps":[]}]"""
        val cachedPeriodReturnsJson = """{"ONE_WEEK":1.5,"ONE_MONTH":3.2,"ONE_YEAR":15.0}"""
        every { sharedPreferences.getString("dashboard_cached_stocks_json", null) } returns cachedStocksJson
        every { sharedPreferences.getString("dashboard_cached_period_returns", null) } returns cachedPeriodReturnsJson
        // Empty holdings = empty portfolio, should not show cached returns
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        val state = viewModel.uiState.value as DashboardUiState.Success

        // Empty portfolio should have empty period returns (showing cached data would be misleading)
        assertThat(state.periodReturns).isEmpty()
    }

    @Test
    fun `cold start - invalid cached period returns - does not crash`() = runTest {
        // Test that invalid cached JSON is handled gracefully
        val cachedStocksJson = """[{"id":1,"symbol":"AAPL","name":"Apple","quantity":10,"averagePrice":100.0,"currentPrice":150.0,"currency":"USD","accountDetails":[],"priceHistory":[],"priceHistoryTimestamps":[]}]"""
        every { sharedPreferences.getString("dashboard_cached_stocks_json", null) } returns cachedStocksJson
        every { sharedPreferences.getString("dashboard_cached_period_returns", null) } returns "invalid json"
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        // Should not crash with invalid cache JSON
        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        val state = viewModel.uiState.value as DashboardUiState.Success

        // Invalid cache should result in empty map (graceful degradation)
        assertThat(state.periodReturns).isEmpty()
    }

    @Test
    fun `loadAllPeriodReturns - saves period returns to cache for all accounts`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0))
        )
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(100.0, 110.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)

        // Verify period returns are saved to cache
        io.mockk.verify { sharedPreferencesEditor.putString("dashboard_cached_period_returns", any()) }
    }

    @Test
    fun `loadAllPeriodReturns - single account view - does not cache period returns`() = runTest {
        val account = AccountEntity(1, "Default", 1000L)
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(account))
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        every { holdingsRepository.getHoldingsByAccount(1L) } returns flowOf(
            listOf(HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 100.0, "USD"))
        )
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(
            listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0))
        )
        coEvery { accountRepository.getAccountById(1L) } returns account
        val day1 = 1704067200L
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(100.0, 110.0),
                timestamps = listOf(day1, day1 + 86400)
            )
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.selectAccount(1L)

        // Verify period returns are NOT saved for single account view
        io.mockk.verify(exactly = 0) { sharedPreferencesEditor.putString("dashboard_cached_period_returns", any()) }
    }

    @Test
    fun `addCashItem - calls repository with correct parameters`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        coEvery { cashRepository.addCashItem(any(), any(), any(), any(), any()) } returns 1L

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, cashRepository, sharedPreferences, portfolioCache)
        viewModel.addCashItem(1L, "Emergency Fund", 10000.0, 4.5, "USD")

        coVerify { cashRepository.addCashItem(1L, "Emergency Fund", 10000.0, 4.5, "USD") }
    }
}
