package com.portfolio.manager.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class YahooChartResponse(
    val chart: ChartData
)

@Serializable
data class ChartData(
    val result: List<ChartResult>? = null,
    val error: ChartError? = null
)

@Serializable
data class ChartResult(
    val meta: ChartMeta,
    val timestamp: List<Long>? = null,
    val indicators: ChartIndicators? = null,
    val events: ChartEvents? = null
)

@Serializable
data class ChartEvents(
    val dividends: Map<String, DividendEvent>? = null
)

@Serializable
data class DividendEvent(
    val amount: Double,
    val date: Long
)

@Serializable
data class ChartMeta(
    val symbol: String,
    val shortName: String? = null,
    val longName: String? = null,
    val regularMarketPrice: Double = 0.0,
    val chartPreviousClose: Double = 0.0,
    val currency: String = "USD",
    val trailingAnnualDividendRate: Double? = null,
    val trailingAnnualDividendYield: Double? = null
)

@Serializable
data class ChartIndicators(
    val quote: List<ChartQuote>? = null
)

@Serializable
data class ChartQuote(
    val close: List<Double?>? = null
)

@Serializable
data class ChartError(
    val code: String? = null,
    val description: String? = null
)
