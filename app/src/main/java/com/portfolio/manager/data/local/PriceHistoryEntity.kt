package com.portfolio.manager.data.local

import androidx.room.Entity

@Entity(tableName = "price_history", primaryKeys = ["symbol", "range"])
data class PriceHistoryEntity(
    val symbol: String,
    val range: String,
    val prices: String, // JSON array of doubles
    val timestamps: String = "[]", // JSON array of longs (Unix epoch seconds)
    val lastUpdatedDate: String // "yyyy-MM-dd" format
)
