package com.portfolio.manager.domain.model

import kotlinx.serialization.Serializable

/**
 * Wire-format snapshot of a single household member's portfolio definition.
 * Prices/returns/history are intentionally excluded — each device computes
 * those locally from Yahoo Finance.
 */
@Serializable
data class PortfolioSnapshot(
    val displayName: String,
    val updatedAt: Long = 0L,
    val schemaVersion: Int = 1,
    val accounts: List<SnapshotAccount> = emptyList(),
    val holdings: List<SnapshotHolding> = emptyList(),
    val cashItems: List<SnapshotCashItem> = emptyList()
)

@Serializable
data class SnapshotAccount(
    val localId: Long,
    val name: String,
    val order: Int
)

@Serializable
data class SnapshotHolding(
    val localId: Long,
    val accountLocalId: Long,
    val symbol: String,
    val name: String,
    val quantity: Int,
    val averagePrice: Double,
    val currency: Currency,
    val targetPercentage: Int? = null
)

@Serializable
data class SnapshotCashItem(
    val localId: Long,
    val accountLocalId: Long,
    val name: String,
    val value: Double,
    val annualYieldRate: Double,
    val currency: Currency
)
