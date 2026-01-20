package com.portfolio.manager.domain.repository

import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.model.PeriodReturn
import com.portfolio.manager.domain.model.TimePeriod

interface StockRepository {
    suspend fun getQuotes(symbols: List<String>): Result<List<QuoteResult>>
    suspend fun getPeriodReturn(symbol: String, period: TimePeriod): Result<PeriodReturn>
    suspend fun getExchangeRate(from: String, to: String): Result<Double>
}
