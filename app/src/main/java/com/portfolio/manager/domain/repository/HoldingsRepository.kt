package com.portfolio.manager.domain.repository

import com.portfolio.manager.data.local.HoldingEntity
import kotlinx.coroutines.flow.Flow

interface HoldingsRepository {
    fun getAllHoldings(): Flow<List<HoldingEntity>>
    suspend fun getHoldingById(id: Long): HoldingEntity?
    suspend fun addHolding(holding: HoldingEntity): Long
    suspend fun updateHolding(holding: HoldingEntity)
    suspend fun deleteHolding(id: Long)
}
