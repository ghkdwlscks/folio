package com.portfolio.manager.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.portfolio.manager.data.local.HoldingDao
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.repository.HoldingsRepository

class HoldingsRepositoryImpl(
    private val dao: HoldingDao
) : HoldingsRepository {

    override fun getHoldingsByAccount(accountId: Long): Flow<List<HoldingEntity>> {
        return dao.getHoldingsByAccount(accountId)
    }

    override fun getAllHoldings(): Flow<List<HoldingEntity>> {
        return dao.getAllHoldings()
    }

    override suspend fun getHoldingById(id: Long): HoldingEntity? {
        return dao.getHoldingById(id)
    }

    override suspend fun getHoldingByAccountAndSymbol(accountId: Long, symbol: String): HoldingEntity? {
        return dao.getHoldingByAccountAndSymbol(accountId, symbol)
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

    override suspend fun getHoldingsCountByAccount(accountId: Long): Int {
        return dao.getHoldingsCountByAccount(accountId)
    }

    override fun getHoldingsCountByAccountFlow(): Flow<Map<Long, Int>> {
        return dao.getHoldingsCountByAccountFlow().map { counts ->
            counts.associate { it.accountId to it.count }
        }
    }

    override suspend fun updateTargetPercentage(holdingId: Long, targetPercentage: Int?) {
        dao.updateTargetPercentage(holdingId, targetPercentage)
    }

    override suspend fun getHoldingsByAccountSync(accountId: Long): List<HoldingEntity> {
        return dao.getHoldingsByAccountSync(accountId)
    }
}
