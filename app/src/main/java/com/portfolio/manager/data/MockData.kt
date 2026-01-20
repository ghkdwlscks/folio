package com.portfolio.manager.data

import com.portfolio.manager.domain.model.Stock

object MockData {

    val stocks = listOf(
        // US Stocks
        Stock(
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 15,
            averagePrice = 145.00,
            currentPrice = 178.50
        ),
        Stock(
            symbol = "GOOGL",
            name = "Alphabet Inc.",
            quantity = 8,
            averagePrice = 125.00,
            currentPrice = 141.80
        ),
        Stock(
            symbol = "MSFT",
            name = "Microsoft Corp.",
            quantity = 12,
            averagePrice = 310.00,
            currentPrice = 378.90
        ),
        Stock(
            symbol = "TSLA",
            name = "Tesla Inc.",
            quantity = 5,
            averagePrice = 280.00,
            currentPrice = 248.50
        ),
        Stock(
            symbol = "NVDA",
            name = "NVIDIA Corp.",
            quantity = 10,
            averagePrice = 450.00,
            currentPrice = 875.30
        ),
        // Korean Stocks
        Stock(
            symbol = "005930",
            name = "Samsung Electronics",
            quantity = 50,
            averagePrice = 72000.00,
            currentPrice = 78500.00
        ),
        Stock(
            symbol = "000660",
            name = "SK Hynix",
            quantity = 30,
            averagePrice = 125000.00,
            currentPrice = 178000.00
        )
    )
}
