package com.portfolio.manager.domain.model

import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.data.local.HoldingEntity

/**
 * Combined household data: my entities plus the partner's, with partner IDs
 * namespaced into negative space to avoid collisions with local autoincrement
 * IDs. [partnerAccountIds] marks read-only accounts; [ownerLabels] maps each
 * account id to its owner's display label for the aggregated view.
 */
data class HouseholdMergeResult(
    val accounts: List<AccountEntity>,
    val holdings: List<HoldingEntity>,
    val cashItems: List<CashItemEntity>,
    val partnerAccountIds: Set<Long>,
    val ownerLabels: Map<Long, String>
)
