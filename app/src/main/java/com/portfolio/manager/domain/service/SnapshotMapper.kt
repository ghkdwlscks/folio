package com.portfolio.manager.domain.service

import javax.inject.Inject

import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.domain.model.PortfolioSnapshot
import com.portfolio.manager.domain.model.SnapshotAccount
import com.portfolio.manager.domain.model.SnapshotCashItem
import com.portfolio.manager.domain.model.SnapshotHolding

/**
 * Converts local Room entities into a [PortfolioSnapshot] for publishing.
 */
class SnapshotMapper @Inject constructor() {

    fun toSnapshot(
        displayName: String,
        updatedAt: Long,
        accounts: List<AccountEntity>,
        holdings: List<HoldingEntity>,
        cashItems: List<CashItemEntity>
    ): PortfolioSnapshot = PortfolioSnapshot(
        displayName = displayName,
        updatedAt = updatedAt,
        schemaVersion = 1,
        accounts = accounts.map { SnapshotAccount(localId = it.id, name = it.name, order = it.orderIndex) },
        holdings = holdings.map {
            SnapshotHolding(
                localId = it.id,
                accountLocalId = it.accountId,
                symbol = it.symbol,
                name = it.name,
                quantity = it.quantity,
                averagePrice = it.averagePrice,
                currency = Currency.fromCodeOrNull(it.currency) ?: Currency.USD,
                targetPercentage = it.targetPercentage
            )
        },
        cashItems = cashItems.map {
            SnapshotCashItem(
                localId = it.id,
                accountLocalId = it.accountId,
                name = it.name,
                value = it.originalValue,
                annualYieldRate = it.annualYieldRate,
                currency = Currency.fromCodeOrNull(it.currency) ?: Currency.USD
            )
        }
    )
}
