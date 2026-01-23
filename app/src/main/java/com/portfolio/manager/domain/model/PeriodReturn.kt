package com.portfolio.manager.domain.model

enum class TimePeriod(val label: String, val range: String) {
    ONE_WEEK("1W", "5d"),
    ONE_MONTH("1M", "1mo"),
    THREE_MONTHS("3M", "3mo"),
    SIX_MONTHS("6M", "6mo"),
    ONE_YEAR("1Y", "1y")
}

data class PeriodReturn(
    val symbol: String,
    val period: TimePeriod,
    val returnPercent: Double
)

data class BenchmarkReturns(
    val sp500: Double?,
    val kospi: Double?
)
