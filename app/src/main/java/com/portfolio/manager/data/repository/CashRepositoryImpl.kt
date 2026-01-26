package com.portfolio.manager.data.repository

import com.portfolio.manager.data.local.CashItemDao
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.domain.repository.CashRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CashRepositoryImpl @Inject constructor(
    private val cashItemDao: CashItemDao
) : CashRepository {

    override fun getCashItemsByAccount(accountId: Long): Flow<List<CashItemEntity>> {
        return cashItemDao.getCashItemsByAccount(accountId)
    }

    override fun getAllCashItems(): Flow<List<CashItemEntity>> {
        return cashItemDao.getAllCashItems()
    }

    override suspend fun getCashItemById(id: Long): CashItemEntity? {
        return cashItemDao.getCashItemById(id)
    }

    override suspend fun addCashItem(
        accountId: Long,
        name: String,
        value: Double,
        yieldRate: Double,
        currency: String
    ): Long {
        val cashItem = CashItemEntity(
            accountId = accountId,
            name = name,
            originalValue = value,
            annualYieldRate = yieldRate,
            currency = currency
        )
        return cashItemDao.insert(cashItem)
    }

    override suspend fun updateCashItem(
        id: Long,
        name: String,
        value: Double,
        yieldRate: Double,
        currency: String
    ) {
        val existing = cashItemDao.getCashItemById(id) ?: return
        val updated = existing.copy(
            name = name,
            originalValue = value,
            annualYieldRate = yieldRate,
            currency = currency
        )
        cashItemDao.update(updated)
    }

    override suspend fun deleteCashItem(id: Long) {
        cashItemDao.deleteById(id)
    }
}
