package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.model.BenchmarkReturns
import com.portfolio.manager.domain.model.PeriodReturn
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var sharedPreferencesEditor: SharedPreferences.Editor
    private val testDispatcher = UnconfinedTestDispatcher()

    private val defaultAccount = AccountEntity(1, "Default", 1000L)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        stockRepository = mockk()
        holdingsRepository = mockk()
        accountRepository = mockk()
        sharedPreferences = mockk()
        sharedPreferencesEditor = mockk()

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
    }

    @Test
    fun `empty holdings - returns empty success`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)

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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)

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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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
        coEvery { stockRepository.getPeriodReturn("AAPL", TimePeriod.ONE_MONTH) } returns
            Result.success(PeriodReturn("AAPL", TimePeriod.ONE_MONTH, 5.5))

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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
        coEvery { stockRepository.getPeriodReturn("AAPL", TimePeriod.ONE_WEEK) } returns
            Result.success(PeriodReturn("AAPL", TimePeriod.ONE_WEEK, 10.0))
        coEvery { stockRepository.getPeriodReturn("GOOGL", TimePeriod.ONE_WEEK) } returns
            Result.success(PeriodReturn("GOOGL", TimePeriod.ONE_WEEK, 20.0))

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
        viewModel.selectPeriod(TimePeriod.ONE_WEEK)

        val state = viewModel.uiState.value as DashboardUiState.Success
        // Weighted return: 0.5 * 10% + 0.5 * 20% = 15%
        assertThat(state.periodReturns[TimePeriod.ONE_WEEK]).isWithin(0.01).of(15.0)
    }

    @Test
    fun `selectPeriod - empty stocks - does not load period returns`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
        viewModel.selectPeriod(TimePeriod.SIX_MONTHS)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.periodReturns[TimePeriod.SIX_MONTHS]).isEqualTo(0.0)
    }

    @Test
    fun `init - fetches exchange rate on startup`() = runTest {
        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(1350.0)

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)

        coVerify { stockRepository.getExchangeRate("USD", "KRW") }
        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.exchangeRate).isEqualTo(1350.0)
    }

    @Test
    fun `init - exchange rate failure - uses default rate`() = runTest {
        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.failure(IOException("Network error"))

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)

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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.exchangeRate).isEqualTo(1300.0)
    }

    @Test
    fun `toggleCurrency - updates showInKrw state for all accounts`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)

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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)

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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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
        coEvery { stockRepository.getPeriodReturn("AAPL", TimePeriod.ONE_MONTH) } returns
            Result.success(PeriodReturn("AAPL", TimePeriod.ONE_MONTH, 5.0))
        coEvery { stockRepository.getPeriodReturn("^GSPC", TimePeriod.ONE_MONTH) } returns
            Result.success(PeriodReturn("^GSPC", TimePeriod.ONE_MONTH, 3.0))
        coEvery { stockRepository.getPeriodReturn("^KS11", TimePeriod.ONE_MONTH) } returns
            Result.success(PeriodReturn("^KS11", TimePeriod.ONE_MONTH, 2.0))

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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
        coEvery { stockRepository.getPeriodReturn("AAPL", TimePeriod.ONE_YEAR) } returns
            Result.success(PeriodReturn("AAPL", TimePeriod.ONE_YEAR, 10.0))
        coEvery { stockRepository.getPeriodReturn("^GSPC", TimePeriod.ONE_YEAR) } returns
            Result.failure(Exception("Network error"))
        coEvery { stockRepository.getPeriodReturn("^KS11", TimePeriod.ONE_YEAR) } returns
            Result.success(PeriodReturn("^KS11", TimePeriod.ONE_YEAR, 5.0))

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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
        coEvery { stockRepository.getPeriodReturn("AAPL", TimePeriod.ONE_YEAR) } returns
            Result.success(PeriodReturn("AAPL", TimePeriod.ONE_YEAR, 10.0))
        coEvery { stockRepository.getPeriodReturn("^GSPC", TimePeriod.ONE_YEAR) } returns
            Result.failure(Exception("Network error"))
        coEvery { stockRepository.getPeriodReturn("^KS11", TimePeriod.ONE_YEAR) } returns
            Result.failure(Exception("Network error"))

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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
        coEvery { stockRepository.getPeriodReturn("AAPL", TimePeriod.ONE_MONTH) } returns
            Result.success(PeriodReturn("AAPL", TimePeriod.ONE_MONTH, 10.0))
        coEvery { stockRepository.getPeriodReturn("^GSPC", TimePeriod.ONE_MONTH) } returns
            Result.success(PeriodReturn("^GSPC", TimePeriod.ONE_MONTH, 10.0))
        coEvery { stockRepository.getPeriodReturn("^KS11", TimePeriod.ONE_MONTH) } returns
            Result.success(PeriodReturn("^KS11", TimePeriod.ONE_MONTH, 5.0))
        // Exchange rate went from 1300 to 1400 = ~7.7% increase
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns PriceHistoryData(
            prices = listOf(1300.0, 1400.0),
            timestamps = listOf(1000L, 2000L)
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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
        coEvery { stockRepository.getPeriodReturn("005930.KS", TimePeriod.ONE_MONTH) } returns
            Result.success(PeriodReturn("005930.KS", TimePeriod.ONE_MONTH, 10.0))
        coEvery { stockRepository.getPeriodReturn("^GSPC", TimePeriod.ONE_MONTH) } returns
            Result.success(PeriodReturn("^GSPC", TimePeriod.ONE_MONTH, 5.0))
        coEvery { stockRepository.getPeriodReturn("^KS11", TimePeriod.ONE_MONTH) } returns
            Result.success(PeriodReturn("^KS11", TimePeriod.ONE_MONTH, 10.0))
        // Exchange rate went from 1300 to 1400 (KRW weakened)
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns PriceHistoryData(
            prices = listOf(1300.0, 1400.0),
            timestamps = listOf(1000L, 2000L)
        )

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository, accountRepository, sharedPreferences)
        viewModel.selectPeriod(TimePeriod.ONE_YEAR)

        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.benchmarkReturns).isEmpty()
    }
}
