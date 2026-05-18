package com.portfolio.manager.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val orderIndex: Int = 0,
    val preferredCurrency: String = "KRW",
    val toleranceBandPercent: Int? = null
)
