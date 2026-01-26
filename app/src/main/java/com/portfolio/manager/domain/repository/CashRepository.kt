package com.portfolio.manager.domain.repository

import com.portfolio.manager.data.local.CashItemEntity
import kotlinx.coroutines.flow.Flow

interface CashRepository {
    fun getCashItemsByAccount(accountId: Long): Flow<List<CashItemEntity>>
    fun getAllCashItems(): Flow<List<CashItemEntity>>
    suspend fun getCashItemById(id: Long): CashItemEntity?
    suspend fun addCashItem(accountId: Long, name: String, value: Double, yieldRate: Double, currency: String): Long
    suspend fun updateCashItem(id: Long, name: String, value: Double, yieldRate: Double, currency: String)
    suspend fun deleteCashItem(id: Long)
}
