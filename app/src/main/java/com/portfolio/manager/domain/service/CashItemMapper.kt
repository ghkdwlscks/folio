package com.portfolio.manager.domain.service

import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.domain.model.CashItem

/**
 * Service for mapping between CashItemEntity and CashItem domain model.
 * Provides symmetric conversion pattern matching StockMapper.
 */
object CashItemMapper {

    /**
     * Converts a CashItemEntity to a CashItem domain model.
     */
    fun fromEntity(entity: CashItemEntity): CashItem {
        return CashItem(
            id = entity.id,
            accountId = entity.accountId,
            name = entity.name,
            originalValue = entity.originalValue,
            annualYieldRate = entity.annualYieldRate,
            currency = entity.currency,
            createdAt = entity.createdAt
        )
    }

    /**
     * Converts a list of CashItemEntity to CashItem domain models.
     */
    fun fromEntities(entities: List<CashItemEntity>): List<CashItem> {
        return entities.map { fromEntity(it) }
    }
}
