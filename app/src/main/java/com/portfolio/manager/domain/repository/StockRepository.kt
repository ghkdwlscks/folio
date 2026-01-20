package com.portfolio.manager.domain.repository

import com.portfolio.manager.data.remote.dto.QuoteResult

interface StockRepository {
    suspend fun getQuotes(symbols: List<String>): Result<List<QuoteResult>>
}
