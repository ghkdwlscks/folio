package com.portfolio.manager.presentation.viewmodel

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.StockRepository
import io.mockk.coEvery
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
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        stockRepository = mockk()
        holdingsRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `empty holdings - returns empty success`() = runTest {
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository)

        assertThat(viewModel.uiState.value).isInstanceOf(DashboardUiState.Success::class.java)
        val state = viewModel.uiState.value as DashboardUiState.Success
        assertThat(state.stocks).isEmpty()
    }

    @Test
    fun `loadPrices - success - updates stocks with real prices`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, "AAPL", "Apple Inc.", 10, 150.0, "USD")
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository)
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
            HoldingEntity(1, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.failure(IOException("Network error"))

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository)

        assertThat(viewModel.uiState.value).isInstanceOf(DashboardUiState.Error::class.java)
        val errorState = viewModel.uiState.value as DashboardUiState.Error
        assertThat(errorState.message).contains("Network error")
    }

    @Test
    fun `refresh - reloads prices`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, "AAPL", "Apple Inc.", 10, 150.0, "USD")
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

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository)
        viewModel.refresh()

        val state = viewModel.uiState.value as DashboardUiState.Success
        val appleStock = state.stocks.find { it.symbol == "AAPL" }
        assertThat(appleStock?.currentPrice).isEqualTo(180.00)
    }

    @Test
    fun `loadPrices - korean stocks - uses korean name from holdings`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, "005930.KS", "삼성전자", 50, 72000.0, "KRW")
        )
        val quotes = listOf(
            QuoteResult(
                symbol = "005930.KS",
                shortName = "Samsung Electronics",
                regularMarketPrice = 78500.0,
                regularMarketChange = 500.0,
                regularMarketChangePercent = 0.64,
                currency = "KRW"
            )
        )
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = DashboardViewModel(stockRepository, holdingsRepository)
        val state = viewModel.uiState.value as DashboardUiState.Success

        val samsungStock = state.stocks.find { it.symbol == "005930.KS" }
        assertThat(samsungStock).isNotNull()
        assertThat(samsungStock?.name).isEqualTo("삼성전자")
        assertThat(samsungStock?.currency).isEqualTo("KRW")
        assertThat(samsungStock?.currentPrice).isEqualTo(78500.0)
    }
}
