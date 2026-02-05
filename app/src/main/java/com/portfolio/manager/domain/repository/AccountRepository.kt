package com.portfolio.manager.domain.repository

import kotlinx.coroutines.flow.Flow

import com.portfolio.manager.data.local.AccountEntity

interface AccountRepository {
    fun getAllAccounts(): Flow<List<AccountEntity>>
    suspend fun getAccountById(id: Long): AccountEntity?
    suspend fun addAccount(account: AccountEntity): Long
    suspend fun updateAccount(account: AccountEntity)
    suspend fun updateAccounts(accounts: List<AccountEntity>)
    suspend fun deleteAccount(id: Long)
    suspend fun getAccountCount(): Int
    suspend fun getMaxOrderIndex(): Int
    suspend fun getOrCreateDefaultAccount(defaultName: String): AccountEntity
    suspend fun updatePreferredCurrency(accountId: Long, currency: String)
}
