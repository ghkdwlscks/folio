package com.portfolio.manager.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class YahooQuoteResponse(
    val quoteResponse: QuoteResponse
)

@Serializable
data class QuoteResponse(
    val result: List<QuoteResult>,
    val error: String? = null
)

@Serializable
data class QuoteResult(
    val symbol: String,
    val shortName: String? = null,
    val longName: String? = null,
    val regularMarketPrice: Double = 0.0,
    val regularMarketPreviousClose: Double = 0.0,
    val regularMarketChange: Double = 0.0,
    val regularMarketChangePercent: Double = 0.0,
    val currency: String = "USD"
)
