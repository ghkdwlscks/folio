package com.portfolio.manager.domain.repository

import kotlinx.coroutines.flow.Flow

import com.portfolio.manager.data.local.CashItemEntity

interface CashRepository {
    fun getCashItemsByAccount(accountId: Long): Flow<List<CashItemEntity>>
    fun getAllCashItems(): Flow<List<CashItemEntity>>
    fun getCashItemsCountByAccountFlow(): Flow<Map<Long, Int>>
    suspend fun getCashItemById(id: Long): CashItemEntity?
    suspend fun addCashItem(cashItem: CashItemEntity): Long
    suspend fun updateCashItem(cashItem: CashItemEntity)
    suspend fun deleteCashItem(id: Long)
}
