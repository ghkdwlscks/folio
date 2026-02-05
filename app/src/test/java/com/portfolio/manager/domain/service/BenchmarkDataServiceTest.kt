package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.util.AppConstants.BENCHMARK_KOSPI
import com.portfolio.manager.util.AppConstants.BENCHMARK_SP500
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class BenchmarkDataServiceTest {

    private lateinit var stockRepository: StockRepository

    @Before
    fun setup() {
        stockRepository = mockk()
    }

    @Test
    fun `loadBenchmarkData - returns benchmark data for both indices`() = runTest {
        // Setup exchange rate data (stable - no FX change)
        coEvery { stockRepository.getExchangeRateHistory("USD", "KRW", any()) } returns
            PriceHistoryData(listOf(1400.0, 1400.0, 1400.0), listOf(1000L, 2000L, 3000L))

        // Setup S&P 500 data - 10% gain
        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_SP500), any()) } returns
            mapOf(BENCHMARK_SP500 to PriceHistoryData(
                listOf(100.0, 105.0, 110.0),
                listOf(1000L, 2000L, 3000L)
            ))

        // Setup KOSPI data - 5% gain
        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_KOSPI), any()) } returns
            mapOf(BENCHMARK_KOSPI to PriceHistoryData(
                listOf(2500.0, 2550.0, 2625.0),
                listOf(1000L, 2000L, 3000L)
            ))

        val result = BenchmarkDataService.loadBenchmarkData(
            stockRepository,
            TimePeriod.ONE_MONTH,
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        // Verify returns are calculated
        assertThat(result.returns.sp500).isNotNull()
        assertThat(result.returns.kospi).isNotNull()

        // S&P 500 return should be ~10% (no FX adjustment for USD in USD view)
        assertThat(result.returns.sp500!!).isWithin(0.5).of(10.0)
        // KOSPI return should be ~5% (no FX change means no adjustment)
        assertThat(result.returns.kospi!!).isWithin(0.5).of(5.0)

        // Verify sparklines are normalized
        assertThat(result.sparklines[BENCHMARK_SP500]).isNotNull()
        assertThat(result.sparklines[BENCHMARK_SP500]!!.first()).isEqualTo(100.0)

        // Verify timestamps
        assertThat(result.timestamps[BENCHMARK_SP500]).isNotNull()
        assertThat(result.timestamps[BENCHMARK_SP500]).hasSize(3)
    }

    @Test
    fun `loadBenchmarkData - with showInKrw true adjusts USD returns`() = runTest {
        // Exchange rate increased from 1300 to 1400 (USD strengthened ~7.7%)
        coEvery { stockRepository.getExchangeRateHistory("USD", "KRW", any()) } returns
            PriceHistoryData(listOf(1300.0, 1400.0), listOf(1000L, 2000L))

        // S&P 500 data - 10% gain in USD
        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_SP500), any()) } returns
            mapOf(BENCHMARK_SP500 to PriceHistoryData(
                listOf(100.0, 110.0),
                listOf(1000L, 2000L)
            ))

        // KOSPI data - 5% gain
        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_KOSPI), any()) } returns
            mapOf(BENCHMARK_KOSPI to PriceHistoryData(
                listOf(2500.0, 2625.0),
                listOf(1000L, 2000L)
            ))

        val result = BenchmarkDataService.loadBenchmarkData(
            stockRepository,
            TimePeriod.ONE_MONTH,
            showInKrw = true,
            currentExchangeRate = 1400.0
        )

        // S&P 500 return in KRW should include FX gain (~10% + ~7.7% compound)
        assertThat(result.returns.sp500!!).isGreaterThan(15.0)
        // KOSPI return should still be ~5% (no FX adjustment for KRW stocks in KRW view)
        assertThat(result.returns.kospi!!).isWithin(0.5).of(5.0)
    }

    @Test
    fun `loadBenchmarkData - empty price history returns null returns`() = runTest {
        coEvery { stockRepository.getExchangeRateHistory("USD", "KRW", any()) } returns
            PriceHistoryData(emptyList(), emptyList())

        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_SP500), any()) } returns
            mapOf(BENCHMARK_SP500 to PriceHistoryData(emptyList(), emptyList()))

        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_KOSPI), any()) } returns
            mapOf(BENCHMARK_KOSPI to PriceHistoryData(emptyList(), emptyList()))

        val result = BenchmarkDataService.loadBenchmarkData(
            stockRepository,
            TimePeriod.ONE_MONTH,
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        assertThat(result.returns.sp500).isNull()
        assertThat(result.returns.kospi).isNull()
        assertThat(result.sparklines).isEmpty()
        assertThat(result.timestamps).isEmpty()
    }

    @Test
    fun `loadBenchmarkData - single data point returns null returns and no sparklines`() = runTest {
        coEvery { stockRepository.getExchangeRateHistory("USD", "KRW", any()) } returns
            PriceHistoryData(listOf(1400.0), listOf(1000L))

        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_SP500), any()) } returns
            mapOf(BENCHMARK_SP500 to PriceHistoryData(listOf(100.0), listOf(1000L)))

        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_KOSPI), any()) } returns
            mapOf(BENCHMARK_KOSPI to PriceHistoryData(listOf(2500.0), listOf(1000L)))

        val result = BenchmarkDataService.loadBenchmarkData(
            stockRepository,
            TimePeriod.ONE_MONTH,
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        // Single data point = can't calculate return
        assertThat(result.returns.sp500).isNull()
        assertThat(result.returns.kospi).isNull()
        // Single data point = not enough for sparklines
        assertThat(result.sparklines).isEmpty()
        assertThat(result.timestamps).isEmpty()
    }

    @Test
    fun `loadBenchmarkData - missing benchmark returns null for that benchmark`() = runTest {
        coEvery { stockRepository.getExchangeRateHistory("USD", "KRW", any()) } returns
            PriceHistoryData(listOf(1400.0, 1400.0), listOf(1000L, 2000L))

        // S&P 500 data exists
        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_SP500), any()) } returns
            mapOf(BENCHMARK_SP500 to PriceHistoryData(listOf(100.0, 110.0), listOf(1000L, 2000L)))

        // KOSPI data missing
        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_KOSPI), any()) } returns
            emptyMap()

        val result = BenchmarkDataService.loadBenchmarkData(
            stockRepository,
            TimePeriod.ONE_MONTH,
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        assertThat(result.returns.sp500).isNotNull()
        assertThat(result.returns.kospi).isNull()
        assertThat(result.sparklines).containsKey(BENCHMARK_SP500)
        assertThat(result.sparklines).doesNotContainKey(BENCHMARK_KOSPI)
    }

    @Test
    fun `loadBenchmarkData - uses current exchange rate as fallback`() = runTest {
        // Empty exchange rate history
        coEvery { stockRepository.getExchangeRateHistory("USD", "KRW", any()) } returns
            PriceHistoryData(emptyList(), emptyList())

        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_SP500), any()) } returns
            mapOf(BENCHMARK_SP500 to PriceHistoryData(listOf(100.0, 110.0), listOf(1000L, 2000L)))

        coEvery { stockRepository.getPriceHistory(listOf(BENCHMARK_KOSPI), any()) } returns
            mapOf(BENCHMARK_KOSPI to PriceHistoryData(listOf(2500.0, 2625.0), listOf(1000L, 2000L)))

        val result = BenchmarkDataService.loadBenchmarkData(
            stockRepository,
            TimePeriod.ONE_MONTH,
            showInKrw = true,
            currentExchangeRate = 1400.0
        )

        // With no FX change (same start/end rate), return should be raw asset return
        assertThat(result.returns.sp500!!).isWithin(0.5).of(10.0)
    }
}
