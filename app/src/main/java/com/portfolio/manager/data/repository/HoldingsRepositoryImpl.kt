package com.portfolio.manager.data.repository

import com.portfolio.manager.data.local.HoldingDao
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.repository.HoldingsRepository
import kotlinx.coroutines.flow.Flow

class HoldingsRepositoryImpl(
    private val dao: HoldingDao
) : HoldingsRepository {

    override fun getAllHoldings(): Flow<List<HoldingEntity>> {
        return dao.getAllHoldings()
    }

    override suspend fun getHoldingById(id: Long): HoldingEntity? {
        return dao.getHoldingById(id)
    }

    override suspend fun addHolding(holding: HoldingEntity): Long {
        return dao.insert(holding)
    }

    override suspend fun updateHolding(holding: HoldingEntity) {
        dao.update(holding)
    }

    override suspend fun deleteHolding(id: Long) {
        dao.deleteById(id)
    }
}
