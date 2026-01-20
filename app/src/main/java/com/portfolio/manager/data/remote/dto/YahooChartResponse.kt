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
    val meta: ChartMeta
)

@Serializable
data class ChartMeta(
    val symbol: String,
    val shortName: String? = null,
    val longName: String? = null,
    val regularMarketPrice: Double = 0.0,
    val chartPreviousClose: Double = 0.0,
    val currency: String = "USD"
)

@Serializable
data class ChartError(
    val code: String? = null,
    val description: String? = null
)
