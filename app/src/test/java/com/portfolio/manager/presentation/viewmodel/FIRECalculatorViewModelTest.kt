package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.repository.CashRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.util.AppConstants.KRW_TO_USD_RATE
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

@OptIn(ExperimentalCoroutinesApi::class)
class FIRECalculatorViewModelTest {

    private lateinit var stockRepository: StockRepository
    private lateinit var holdingsRepository: HoldingsRepository
    private lateinit var cashRepository: CashRepository
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val testDispatcher = UnconfinedTestDispatcher()

    // Mutable storage to track SharedPreferences values (doubles stored as strings for precision)
    private val prefValues = mutableMapOf<String, Any>(
        "fire_annual_return" to "7.0",
        "fire_annual_inflation" to "2.0",
        "fire_target_monthly_spending" to "3000.0",
        "fire_show_in_krw" to false
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        stockRepository = mockk()
        holdingsRepository = mockk()
        cashRepository = mockk()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)

        // Default: no cash items
        every { cashRepository.getAllCashItems() } returns flowOf(emptyList())

        every { sharedPreferences.edit() } returns editor
        every { editor.putString(any(), any()) } answers {
            prefValues[firstArg()] = secondArg<String>()
            editor
        }
        every { editor.putBoolean(any(), any()) } answers {
            prefValues[firstArg()] = secondArg<Boolean>()
            editor
        }

        // Return values from our mutable storage (doubles use getString)
        every { sharedPreferences.getString("fire_annual_return", null) } answers {
            prefValues["fire_annual_return"] as? String
        }
        every { sharedPreferences.getString("fire_annual_inflation", null) } answers {
            prefValues["fire_annual_inflation"] as? String
        }
        every { sharedPreferences.getString("fire_target_monthly_spending", null) } answers {
            prefValues["fire_target_monthly_spending"] as? String
        }
        every { sharedPreferences.getBoolean("fire_show_in_krw", any()) } answers {
            prefValues["fire_show_in_krw"] as Boolean
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FIRECalculatorViewModel {
        return FIRECalculatorViewModel(stockRepository, holdingsRepository, cashRepository, sharedPreferences)
    }

    @Test
    fun `initial state - empty holdings returns zero portfolio value`() = runTest {
        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value).isInstanceOf(FIRECalculatorUiState.Success::class.java)
        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        assertThat(state.totalPortfolioValue).isEqualTo(0.0)
    }

    @Test
    fun `initial state - calculates portfolio value from holdings`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 10, 150.0, "USD")
        )
        val quotes = listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 200.0, longName = "Apple"))

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value).isInstanceOf(FIRECalculatorUiState.Success::class.java)
        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        assertThat(state.totalPortfolioValue).isEqualTo(2000.0) // 10 * 200
    }

    @Test
    fun `calculateFIRE - with positive real return`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 100, 100.0, "USD")
        )
        val quotes = listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0, longName = "Apple"))

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        // 7% return - 2% inflation = 5% real return
        val viewModel = createViewModel()

        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        // Portfolio = $10,000, Real return = 5%
        // Sustainable annual = $10,000 * 0.05 = $500
        // Sustainable monthly = $500 / 12 = $41.67
        assertThat(state.fireCalculation.realReturn).isEqualTo(5.0)
        assertThat(state.fireCalculation.sustainableAnnualSpending).isWithin(0.01).of(500.0)
        assertThat(state.fireCalculation.sustainableMonthlySpending).isWithin(0.01).of(41.67)
    }

    @Test
    fun `calculateFIRE - with zero real return`() = runTest {
        prefValues["fire_annual_return"] = "3.0"
        prefValues["fire_annual_inflation"] = "3.0"

        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 100, 100.0, "USD")
        )
        val quotes = listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0, longName = "Apple"))

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        assertThat(state.fireCalculation.realReturn).isEqualTo(0.0)
        assertThat(state.fireCalculation.sustainableAnnualSpending).isEqualTo(0.0)
        assertThat(state.fireCalculation.sustainableMonthlySpending).isEqualTo(0.0)
    }

    @Test
    fun `calculateFIRETarget - calculates required portfolio`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 100, 100.0, "USD")
        )
        val quotes = listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0, longName = "Apple"))

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        // Target monthly = $3000 -> annual = $36000
        // Real return = 5%
        // Required portfolio = $36000 / 0.05 = $720,000
        val viewModel = createViewModel()

        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        assertThat(state.fireTargetCalculation.requiredPortfolio).isWithin(0.01).of(720000.0)
        // Progress = $10,000 / $720,000 * 100 = 1.39%
        assertThat(state.fireTargetCalculation.progressPercent).isWithin(0.1).of(1.39)
        // Remaining = $720,000 - $10,000 = $710,000
        assertThat(state.fireTargetCalculation.remainingAmount).isWithin(0.01).of(710000.0)
    }

    @Test
    fun `updateAnnualReturn - recalculates FIRE values`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 100, 100.0, "USD")
        )
        val quotes = listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0, longName = "Apple"))

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = createViewModel()

        // Change annual return from 7% to 10%
        viewModel.updateAnnualReturn(10.0)

        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        assertThat(state.annualReturn).isEqualTo(10.0)
        // Real return = 10% - 2% = 8%
        assertThat(state.fireCalculation.realReturn).isEqualTo(8.0)
    }

    @Test
    fun `updateAnnualInflation - recalculates FIRE values`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 100, 100.0, "USD")
        )
        val quotes = listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0, longName = "Apple"))

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = createViewModel()

        // Change inflation from 2% to 4%
        viewModel.updateAnnualInflation(4.0)

        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        assertThat(state.annualInflation).isEqualTo(4.0)
        // Real return = 7% - 4% = 3%
        assertThat(state.fireCalculation.realReturn).isEqualTo(3.0)
    }

    @Test
    fun `updateTargetMonthlySpending - recalculates target values`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 100, 100.0, "USD")
        )
        val quotes = listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0, longName = "Apple"))

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = createViewModel()

        // Change target from $3000 to $5000/month
        viewModel.updateTargetMonthlySpending(5000.0)

        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        assertThat(state.targetMonthlySpending).isEqualTo(5000.0)
        // Required = $60000 / 0.05 = $1,200,000
        assertThat(state.fireTargetCalculation.requiredPortfolio).isWithin(0.01).of(1200000.0)
    }

    @Test
    fun `toggleCurrency - switches between USD and KRW`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 10, 100.0, "USD")
        )
        val quotes = listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0, longName = "Apple"))

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(1400.0)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = createViewModel()

        // Initially in USD
        var state = viewModel.uiState.value as FIRECalculatorUiState.Success
        assertThat(state.showInKrw).isFalse()
        assertThat(state.totalPortfolioValue).isEqualTo(1000.0)
        assertThat(state.targetMonthlySpending).isEqualTo(3000.0) // $3000

        // Toggle to KRW
        viewModel.toggleCurrency()

        state = viewModel.uiState.value as FIRECalculatorUiState.Success
        assertThat(state.showInKrw).isTrue()
        assertThat(state.totalPortfolioValue).isEqualTo(1400000.0) // $1000 * 1400
        assertThat(state.targetMonthlySpending).isEqualTo(4200000.0) // $3000 * 1400
    }

    @Test
    fun `KRW holdings - converted correctly to USD`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "005930.KS", "Samsung", 10, 70000.0, "KRW")
        )
        val quotes = listOf(QuoteResult(symbol = "005930.KS", regularMarketPrice = 70000.0, longName = "Samsung"))

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(1400.0)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        // 10 shares * 70,000 KRW = 700,000 KRW
        // 700,000 KRW / 1400 = $500
        assertThat(state.totalPortfolioValue).isWithin(0.01).of(500.0)
    }

    @Test
    fun `error state - when stock prices fail to load`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 10, 100.0, "USD")
        )

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.failure(Exception("Network error"))

        val viewModel = createViewModel()

        assertThat(viewModel.uiState.value).isInstanceOf(FIRECalculatorUiState.Error::class.java)
    }

    @Test
    fun `calculateFIRETarget - with negative real return returns infinity`() = runTest {
        prefValues["fire_annual_return"] = "2.0"
        prefValues["fire_annual_inflation"] = "5.0"

        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 100, 100.0, "USD")
        )
        val quotes = listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0, longName = "Apple"))

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        // Real return = 2% - 5% = -3% (negative)
        assertThat(state.fireCalculation.realReturn).isEqualTo(-3.0)
        assertThat(state.fireTargetCalculation.requiredPortfolio).isPositiveInfinity()
        assertThat(state.fireTargetCalculation.progressPercent).isEqualTo(0.0)
    }

    @Test
    fun `portfolio value - includes cash items in USD`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 10, 100.0, "USD")
        )
        val quotes = listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0, longName = "Apple"))
        val cashItems = listOf(
            CashItemEntity(1, 1L, "Emergency Fund", 5000.0, 4.0, "USD", 0L)
        )

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        every { cashRepository.getAllCashItems() } returns flowOf(cashItems)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        // Stocks: 10 * $100 = $1,000
        // Cash: $5,000
        // Total: $6,000
        assertThat(state.totalPortfolioValue).isEqualTo(6000.0)
    }

    @Test
    fun `portfolio value - includes cash items in KRW converted to USD`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple", 10, 100.0, "USD")
        )
        val quotes = listOf(QuoteResult(symbol = "AAPL", regularMarketPrice = 100.0, longName = "Apple"))
        val cashItems = listOf(
            CashItemEntity(1, 1L, "Korean Savings", 1400000.0, 3.5, "KRW", 0L)
        )

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(1400.0)
        every { holdingsRepository.getAllHoldings() } returns flowOf(holdings)
        every { cashRepository.getAllCashItems() } returns flowOf(cashItems)
        coEvery { stockRepository.getQuotes(any()) } returns Result.success(quotes)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        // Stocks: 10 * $100 = $1,000
        // Cash: 1,400,000 KRW / 1400 = $1,000
        // Total: $2,000
        assertThat(state.totalPortfolioValue).isWithin(0.01).of(2000.0)
    }

    @Test
    fun `portfolio value - cash only with no holdings`() = runTest {
        val cashItems = listOf(
            CashItemEntity(1, 1L, "Savings", 10000.0, 5.0, "USD", 0L)
        )

        coEvery { stockRepository.getExchangeRate(any(), any()) } returns Result.success(KRW_TO_USD_RATE)
        every { holdingsRepository.getAllHoldings() } returns flowOf(emptyList())
        every { cashRepository.getAllCashItems() } returns flowOf(cashItems)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as FIRECalculatorUiState.Success
        assertThat(state.totalPortfolioValue).isEqualTo(10000.0)
    }
}
