package com.portfolio.manager.data.repository

import com.portfolio.manager.data.remote.YahooFinanceApi
import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.repository.StockRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class StockRepositoryImpl(
    private val api: YahooFinanceApi
) : StockRepository {

    override suspend fun getQuotes(symbols: List<String>): Result<List<QuoteResult>> {
        if (symbols.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            coroutineScope {
                val results = symbols.map { symbol ->
                    async {
                        try {
                            val response = api.getChart(symbol)
                            val meta = response.chart.result?.firstOrNull()?.meta
                            if (meta != null) {
                                QuoteResult(
                                    symbol = meta.symbol,
                                    shortName = meta.shortName,
                                    longName = meta.longName,
                                    regularMarketPrice = meta.regularMarketPrice,
                                    regularMarketPreviousClose = meta.chartPreviousClose,
                                    regularMarketChange = meta.regularMarketPrice - meta.chartPreviousClose,
                                    regularMarketChangePercent = if (meta.chartPreviousClose > 0) {
                                        ((meta.regularMarketPrice - meta.chartPreviousClose) / meta.chartPreviousClose) * 100
                                    } else 0.0,
                                    currency = meta.currency
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                val quotes = results.awaitAll().filterNotNull()
                Result.success(quotes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
