package com.portfolio.manager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val name: String,
    val quantity: Int,
    val averagePrice: Double,
    val currency: String = "USD"
)
