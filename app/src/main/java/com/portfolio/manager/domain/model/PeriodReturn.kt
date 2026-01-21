package com.portfolio.manager.domain.model

enum class TimePeriod(val label: String, val range: String) {
    ONE_WEEK("1W", "5d"),
    ONE_MONTH("1M", "1mo"),
    SIX_MONTHS("6M", "6mo"),
    ONE_YEAR("1Y", "1y"),
    MAX("Max", "max")
}

data class PeriodReturn(
    val symbol: String,
    val period: TimePeriod,
    val returnPercent: Double
)
