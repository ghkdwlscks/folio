package com.portfolio.manager.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BenchmarkReturnsTest {

    @Test
    fun `default values - both null`() {
        val benchmarks = BenchmarkReturns(null, null)

        assertThat(benchmarks.sp500).isNull()
        assertThat(benchmarks.kospi).isNull()
    }

    @Test
    fun `partial data - sp500 only`() {
        val benchmarks = BenchmarkReturns(sp500 = 5.5, kospi = null)

        assertThat(benchmarks.sp500).isEqualTo(5.5)
        assertThat(benchmarks.kospi).isNull()
    }

    @Test
    fun `partial data - kospi only`() {
        val benchmarks = BenchmarkReturns(sp500 = null, kospi = 3.2)

        assertThat(benchmarks.sp500).isNull()
        assertThat(benchmarks.kospi).isEqualTo(3.2)
    }

    @Test
    fun `full data - both benchmarks available`() {
        val benchmarks = BenchmarkReturns(sp500 = 10.2, kospi = 8.5)

        assertThat(benchmarks.sp500).isEqualTo(10.2)
        assertThat(benchmarks.kospi).isEqualTo(8.5)
    }

    @Test
    fun `negative returns - handled correctly`() {
        val benchmarks = BenchmarkReturns(sp500 = -3.5, kospi = -7.2)

        assertThat(benchmarks.sp500).isEqualTo(-3.5)
        assertThat(benchmarks.kospi).isEqualTo(-7.2)
    }

    @Test
    fun `zero returns - handled correctly`() {
        val benchmarks = BenchmarkReturns(sp500 = 0.0, kospi = 0.0)

        assertThat(benchmarks.sp500).isEqualTo(0.0)
        assertThat(benchmarks.kospi).isEqualTo(0.0)
    }

    @Test
    fun `mixed returns - positive and negative`() {
        val benchmarks = BenchmarkReturns(sp500 = 5.0, kospi = -2.5)

        assertThat(benchmarks.sp500).isEqualTo(5.0)
        assertThat(benchmarks.kospi).isEqualTo(-2.5)
    }
}
