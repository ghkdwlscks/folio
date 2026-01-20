package com.portfolio.manager.data.repository

import com.portfolio.manager.data.local.AccountDao
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow

class AccountRepositoryImpl(
    private val dao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<AccountEntity>> {
        return dao.getAllAccounts()
    }

    override suspend fun getAccountById(id: Long): AccountEntity? {
        return dao.getAccountById(id)
    }

    override suspend fun addAccount(account: AccountEntity): Long {
        return dao.insert(account)
    }

    override suspend fun updateAccount(account: AccountEntity) {
        dao.update(account)
    }

    override suspend fun updateAccounts(accounts: List<AccountEntity>) {
        dao.updateAll(accounts)
    }

    override suspend fun deleteAccount(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun getAccountCount(): Int {
        return dao.getAccountCount()
    }

    override suspend fun getMaxOrderIndex(): Int {
        return dao.getMaxOrderIndex()
    }
}
