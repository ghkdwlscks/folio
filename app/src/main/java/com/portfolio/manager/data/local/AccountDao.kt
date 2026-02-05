package com.portfolio.manager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY orderIndex ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Update
    suspend fun updateAll(accounts: List<AccountEntity>)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    @Query("SELECT COALESCE(MAX(orderIndex), 0) FROM accounts")
    suspend fun getMaxOrderIndex(): Int

    @Query("SELECT * FROM accounts LIMIT 1")
    suspend fun getFirstAccount(): AccountEntity?

    @Query("UPDATE accounts SET preferredCurrency = :currency WHERE id = :accountId")
    suspend fun updatePreferredCurrency(accountId: Long, currency: String)

    @Transaction
    suspend fun getOrCreateDefaultAccount(defaultName: String): AccountEntity {
        val existing = getFirstAccount()
        if (existing != null) {
            return existing
        }
        val newAccount = AccountEntity(name = defaultName, orderIndex = 0)
        val id = insert(newAccount)
        return newAccount.copy(id = id)
    }
}
