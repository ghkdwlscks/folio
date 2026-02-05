package com.portfolio.manager.data.repository

import javax.inject.Inject

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import com.portfolio.manager.data.local.CashItemDao
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.domain.repository.CashRepository

class CashRepositoryImpl @Inject constructor(
    private val cashItemDao: CashItemDao
) : CashRepository {

    override fun getCashItemsByAccount(accountId: Long): Flow<List<CashItemEntity>> {
        return cashItemDao.getCashItemsByAccount(accountId)
    }

    override fun getAllCashItems(): Flow<List<CashItemEntity>> {
        return cashItemDao.getAllCashItems()
    }

    override fun getCashItemsCountByAccountFlow(): Flow<Map<Long, Int>> {
        return cashItemDao.getCashItemsCountByAccountFlow().map { counts ->
            counts.associate { it.accountId to it.count }
        }
    }

    override suspend fun getCashItemById(id: Long): CashItemEntity? {
        return cashItemDao.getCashItemById(id)
    }

    override suspend fun addCashItem(cashItem: CashItemEntity): Long {
        return cashItemDao.insert(cashItem)
    }

    override suspend fun updateCashItem(cashItem: CashItemEntity) {
        cashItemDao.update(cashItem)
    }

    override suspend fun deleteCashItem(id: Long) {
        cashItemDao.deleteById(id)
    }
}
