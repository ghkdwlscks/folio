package com.portfolio.manager.domain.service

import javax.inject.Inject

import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.HouseholdMergeResult
import com.portfolio.manager.domain.model.PortfolioSnapshot

/**
 * Merges my local entities with a partner's read-only snapshot into a single
 * entity-shaped result for the household aggregated view. Partner IDs are
 * mapped into negative space so they never collide with local autoincrement IDs.
 *
 * Single-partner assumption: one partner snapshot. The negation scheme keeps
 * partner ids distinct from mine; extending to N partners would offset per member.
 */
class HouseholdMerger @Inject constructor() {

    fun merge(
        myAccounts: List<AccountEntity>,
        myHoldings: List<HoldingEntity>,
        myCashItems: List<CashItemEntity>,
        myLabel: String,
        partnerSnapshot: PortfolioSnapshot?
    ): HouseholdMergeResult {
        val ownerLabels = HashMap<Long, String>()
        myAccounts.forEach { ownerLabels[it.id] = myLabel }

        if (partnerSnapshot == null) {
            return HouseholdMergeResult(
                accounts = myAccounts,
                holdings = myHoldings,
                cashItems = myCashItems,
                partnerAccountIds = emptySet(),
                ownerLabels = ownerLabels
            )
        }

        // Map a partner localId into namespaced negative space.
        fun ns(localId: Long): Long = -(localId + 1)

        val partnerAccounts = partnerSnapshot.accounts.map {
            AccountEntity(id = ns(it.localId), name = it.name, orderIndex = it.order)
        }
        partnerAccounts.forEach { ownerLabels[it.id] = partnerSnapshot.displayName }

        val partnerHoldings = partnerSnapshot.holdings.map {
            HoldingEntity(
                id = ns(it.localId),
                accountId = ns(it.accountLocalId),
                symbol = it.symbol,
                name = it.name,
                quantity = it.quantity,
                averagePrice = it.averagePrice,
                currency = it.currency.code,
                targetPercentage = it.targetPercentage
            )
        }

        val partnerCash = partnerSnapshot.cashItems.map {
            CashItemEntity(
                id = ns(it.localId),
                accountId = ns(it.accountLocalId),
                name = it.name,
                originalValue = it.value,
                annualYieldRate = it.annualYieldRate,
                currency = it.currency.code
            )
        }

        return HouseholdMergeResult(
            accounts = myAccounts + partnerAccounts,
            holdings = myHoldings + partnerHoldings,
            cashItems = myCashItems + partnerCash,
            partnerAccountIds = partnerAccounts.map { it.id }.toSet(),
            ownerLabels = ownerLabels
        )
    }
}
