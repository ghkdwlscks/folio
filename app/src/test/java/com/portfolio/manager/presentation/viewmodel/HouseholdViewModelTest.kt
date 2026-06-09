package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.domain.model.PortfolioSnapshot
import com.portfolio.manager.domain.model.SnapshotAccount
import com.portfolio.manager.domain.model.SnapshotHolding
import com.portfolio.manager.domain.model.SortOption
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.CashRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.domain.repository.SyncRepository
import com.portfolio.manager.domain.service.HouseholdMerger
import com.portfolio.manager.domain.service.PortfolioCache
import com.portfolio.manager.domain.service.SnapshotMapper
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HouseholdViewModelTest {

    private lateinit var stockRepository: StockRepository
    private lateinit var holdingsRepository: HoldingsRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var cashRepository: CashRepository
    private lateinit var syncRepository: SyncRepository
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val merger = HouseholdMerger()
    private val snapshotMapper = SnapshotMapper()
    private val portfolioCache = PortfolioCache()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val prefValues = mutableMapOf<String, Any?>("dashboard_show_in_krw" to true)

    private val myHolding = HoldingEntity(
        id = 1L, accountId = 1L, symbol = "AAPL", name = "Apple",
        quantity = 2, averagePrice = 100.0, currency = "USD"
    )
    private val myAccount = AccountEntity(id = 1L, name = "Mine", orderIndex = 0)
    private val myCashEntity = CashItemEntity(
        id = 1L, accountId = 1L, name = "Cash",
        originalValue = 500.0, annualYieldRate = 2.0, currency = "USD"
    )
    private val aaplQuote = QuoteResult(symbol = "AAPL", regularMarketPrice = 150.0, currency = "USD")
    private val googlQuote = QuoteResult(symbol = "GOOGL", regularMarketPrice = 140.0, currency = "USD")

    private val partnerSnapshot = PortfolioSnapshot(
        displayName = "와이프",
        accounts = listOf(SnapshotAccount(localId = 1L, name = "Her", order = 0)),
        holdings = listOf(
            SnapshotHolding(
                localId = 1L, accountLocalId = 1L, symbol = "GOOGL",
                name = "Alphabet", quantity = 3, averagePrice = 100.0, currency = Currency.USD
            )
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        stockRepository = mockk()
        holdingsRepository = mockk()
        accountRepository = mockk()
        cashRepository = mockk()
        syncRepository = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } answers {
            prefValues[firstArg()] = secondArg<Boolean>()
            editor
        }
        every { sharedPreferences.getBoolean("dashboard_show_in_krw", any()) } answers {
            prefValues["dashboard_show_in_krw"] as Boolean
        }
        every { sharedPreferences.getString("household_code", null) } answers {
            prefValues["household_code"] as? String
        }
        every { sharedPreferences.getString("household_my_label", null) } answers {
            prefValues["household_my_label"] as? String
        }
        // Enum/int-backed prefs (e.g. summary period) fall back to their default ordinal
        every { sharedPreferences.getInt(any(), any()) } answers { secondArg() }

        // Default empty data
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())
        every { cashRepository.getAllCashItems() } returns flowOf(emptyList())

        // Default safe network stubs
        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(1300.0)
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns emptyMap()
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns
            PriceHistoryData(emptyList(), emptyList())

        coEvery { syncRepository.publishSnapshot(any(), any(), any()) } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = HouseholdViewModel(
        stockRepository, holdingsRepository, accountRepository, cashRepository,
        syncRepository, merger, snapshotMapper, portfolioCache, sharedPreferences
    )

    private fun successState() = createViewModel().uiState.value as DashboardUiState.Success

    @Test
    fun `load - unpaired empty portfolio - emits empty success`() = runTest {
        val state = successState()

        assertThat(state.stocks).isEmpty()
        assertThat(state.cashItems).isEmpty()
        assertThat(state.showInKrw).isTrue()
    }

    @Test
    fun `load - my holdings only unpaired - aggregates and emits success`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(listOf(myHolding))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { stockRepository.getQuotes(listOf("AAPL")) } returns Result.success(listOf(aaplQuote))

        val state = successState()

        assertThat(state.stocks).hasSize(1)
        assertThat(state.stocks.first().symbol).isEqualTo("AAPL")
        assertThat(state.stocks.first().currentPrice).isEqualTo(150.0)
    }

    @Test
    fun `selectPeriod - updates selected period and recomputes analytics`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(listOf(myHolding))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { stockRepository.getQuotes(listOf("AAPL")) } returns Result.success(listOf(aaplQuote))
        val vm = createViewModel()

        vm.selectPeriod(TimePeriod.ONE_WEEK)

        val state = vm.uiState.value as DashboardUiState.Success
        assertThat(state.selectedPeriod).isEqualTo(TimePeriod.ONE_WEEK)
        assertThat(state.benchmarkReturns.keys).containsExactly(TimePeriod.ONE_WEEK)
    }

    @Test
    fun `selectPeriod - ignored when state is not success`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(listOf(myHolding))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { stockRepository.getQuotes(any()) } returns Result.failure(RuntimeException("boom"))
        val vm = createViewModel()

        vm.selectPeriod(TimePeriod.ONE_WEEK)

        assertThat(vm.uiState.value).isInstanceOf(DashboardUiState.Error::class.java)
    }

    @Test
    fun `load - paired with partner - combines my and partner holdings`() = runTest {
        prefValues["household_code"] = "ABCD-2345"
        prefValues["household_my_label"] = "남편"
        every { holdingsRepository.getAllHoldings() } returns flowOf(listOf(myHolding))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { syncRepository.ensureSignedIn() } returns Result.success("uid-1")
        coEvery { syncRepository.fetchPartnerSnapshot("ABCD-2345", "uid-1") } returns
            Result.success(partnerSnapshot)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(listOf(aaplQuote, googlQuote))

        val state = successState()

        assertThat(state.stocks.map { it.symbol }).containsExactly("AAPL", "GOOGL")
        // Owner labels cover both my account and the partner's namespaced account.
        assertThat(state.ownerLabels.values).containsExactly("남편", "와이프")
    }

    @Test
    fun `load - paired but sign-in fails - shows my data only`() = runTest {
        prefValues["household_code"] = "ABCD-2345"
        every { holdingsRepository.getAllHoldings() } returns flowOf(listOf(myHolding))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { syncRepository.ensureSignedIn() } returns Result.failure(RuntimeException("offline"))
        coEvery { stockRepository.getQuotes(listOf("AAPL")) } returns Result.success(listOf(aaplQuote))

        val state = successState()

        assertThat(state.stocks.map { it.symbol }).containsExactly("AAPL")
    }

    @Test
    fun `load - paired but no partner snapshot - shows my data only`() = runTest {
        prefValues["household_code"] = "ABCD-2345"
        every { holdingsRepository.getAllHoldings() } returns flowOf(listOf(myHolding))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { syncRepository.ensureSignedIn() } returns Result.success("uid-1")
        coEvery { syncRepository.fetchPartnerSnapshot(any(), any()) } returns Result.success(null)
        coEvery { stockRepository.getQuotes(listOf("AAPL")) } returns Result.success(listOf(aaplQuote))

        val state = successState()

        assertThat(state.stocks.map { it.symbol }).containsExactly("AAPL")
    }

    @Test
    fun `load - cash only - emits success without stocks`() = runTest {
        every { cashRepository.getAllCashItems() } returns flowOf(listOf(myCashEntity))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))

        val state = successState()

        assertThat(state.stocks).isEmpty()
        assertThat(state.cashItems).hasSize(1)
        assertThat(state.cashItems.first().name).isEqualTo("Cash")
    }

    @Test
    fun `load - quotes failure - emits error`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(listOf(myHolding))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { stockRepository.getQuotes(any()) } returns Result.failure(RuntimeException("boom"))

        val state = createViewModel().uiState.value

        assertThat(state).isInstanceOf(DashboardUiState.Error::class.java)
    }

    @Test
    fun `toggleCurrency - flips currency and persists`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(listOf(myHolding))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(listOf(aaplQuote))
        val vm = createViewModel()
        assertThat((vm.uiState.value as DashboardUiState.Success).showInKrw).isTrue()

        vm.toggleCurrency()

        assertThat((vm.uiState.value as DashboardUiState.Success).showInKrw).isFalse()
        assertThat(prefValues["dashboard_show_in_krw"]).isEqualTo(false)
    }

    @Test
    fun `selectSortOption - re-sorts and persists`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(listOf(myHolding))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(listOf(aaplQuote))
        val vm = createViewModel()

        vm.selectSortOption(SortOption.NAME)

        assertThat((vm.uiState.value as DashboardUiState.Success).sortOption).isEqualTo(SortOption.NAME)
    }

    @Test
    fun `selectSparklinePeriod - refetches stock price history for the period`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(listOf(myHolding))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(listOf(aaplQuote))
        coEvery { stockRepository.getPriceHistory(listOf("AAPL"), TimePeriod.ONE_WEEK.range) } returns
            mapOf("AAPL" to PriceHistoryData(listOf(1.0, 2.0), listOf(100L, 200L)))
        val vm = createViewModel()

        vm.selectSparklinePeriod(TimePeriod.ONE_WEEK)

        val state = vm.uiState.value as DashboardUiState.Success
        assertThat(state.sparklinePeriod).isEqualTo(TimePeriod.ONE_WEEK)
        assertThat(state.stocks.first().priceHistory).containsExactly(1.0, 2.0).inOrder()
        assertThat(state.isRefreshing).isFalse()
    }

    @Test
    fun `refresh - reloads data`() = runTest {
        val vm = createViewModel()
        assertThat((vm.uiState.value as DashboardUiState.Success).stocks).isEmpty()

        every { holdingsRepository.getAllHoldings() } returns flowOf(listOf(myHolding))
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(listOf(aaplQuote))
        vm.refresh()

        assertThat((vm.uiState.value as DashboardUiState.Success).stocks).hasSize(1)
    }

    @Test
    fun `refresh - existing success - marks refreshing while reload is running`() = runTest {
        val vm = createViewModel()
        val refreshingHoldings = MutableSharedFlow<List<HoldingEntity>>()
        every { holdingsRepository.getAllHoldings() } returns refreshingHoldings
        every { accountRepository.getAllAccounts() } returns flowOf(listOf(myAccount))
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(listOf(aaplQuote))

        vm.refresh()

        assertThat((vm.uiState.value as DashboardUiState.Success).isRefreshing).isTrue()

        refreshingHoldings.emit(listOf(myHolding))

        val state = vm.uiState.value as DashboardUiState.Success
        assertThat(state.isRefreshing).isFalse()
        assertThat(state.stocks).hasSize(1)
    }
}
