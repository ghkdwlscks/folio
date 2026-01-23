package com.portfolio.manager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_names")
data class StockNameEntity(
    @PrimaryKey
    val symbol: String,
    val name: String,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
