package com.portfolio.manager.data.repository

import com.portfolio.manager.data.local.HoldingDao
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.repository.HoldingsRepository
import kotlinx.coroutines.flow.Flow

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
}
