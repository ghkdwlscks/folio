package com.portfolio.manager.domain.service

import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.StockHolding
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.util.ReturnCalculator

data class PortfolioValuesResult(
    val values: List<Double>,
    val validDates: List<String>
)

object PortfolioCalculationService {

    fun buildPortfolioValues(
        stocks: List<Stock>,
        priceHistoryMap: Map<String, PriceHistoryData>,
        exchangeRateData: PriceHistoryData,
        exchangeRate: Double,
        showInKrw: Boolean
    ): PortfolioValuesResult? {
        val stocksWithHistory = stocks.filter { stock ->
            val data = priceHistoryMap[stock.symbol]
            data != null && data.prices.size >= 2
        }
        if (stocksWithHistory.isEmpty()) return null

        val symbolsWithHistory = stocksWithHistory.map { it.symbol }
        val stockDatePrices = PriceHistoryProcessor.buildSymbolDatePrices(symbolsWithHistory, priceHistoryMap)
        val exchangeRateByDate = PriceHistoryProcessor.buildExchangeRateByDate(exchangeRateData)
        val allDates = stockDatePrices.values.flatMap { it.keys }.toSet().sorted()

        if (allDates.size < 2) return null

        val filledStockPrices = PriceHistoryProcessor.forwardFillPrices(symbolsWithHistory, stockDatePrices, allDates)
        val validDates = allDates.filter { date ->
            stocksWithHistory.all { stock ->
                filledStockPrices[stock.symbol]?.containsKey(date) == true
            }
        }

        val filledExchangeRates = PriceHistoryProcessor.forwardFillExchangeRates(
            exchangeRateByDate, validDates, exchangeRate
        )
        val holdings = stocksWithHistory.map { StockHolding(it.symbol, it.quantity, it.currency) }
        val values = PriceHistoryProcessor.calculatePortfolioValues(
            validDates, holdings, filledStockPrices, filledExchangeRates, exchangeRate, showInKrw
        )

        return PortfolioValuesResult(values, validDates)
    }

    fun calculatePeriodReturn(portfolioValues: List<Double>): Double =
        ReturnCalculator.calculateFromListOrZero(portfolioValues)

    fun calculateWeightedCashReturn(cashItems: List<CashItem>, exchangeRate: Double, period: TimePeriod): Double {
        if (cashItems.isEmpty()) return 0.0
        val totalCashValue = cashItems.sumOf { it.valueInUsd(exchangeRate) }
        if (totalCashValue <= 0) return 0.0

        return cashItems.sumOf { cashItem ->
            val weight = cashItem.valueInUsd(exchangeRate) / totalCashValue
            weight * cashItem.periodReturnPercent(period)
        }
    }
}
