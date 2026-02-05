package com.portfolio.manager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

import kotlinx.coroutines.flow.Flow

@Dao
interface CashItemDao {
    @Query("SELECT * FROM cash_items WHERE accountId = :accountId ORDER BY createdAt DESC")
    fun getCashItemsByAccount(accountId: Long): Flow<List<CashItemEntity>>

    @Query("SELECT * FROM cash_items ORDER BY createdAt DESC")
    fun getAllCashItems(): Flow<List<CashItemEntity>>

    @Query("SELECT * FROM cash_items WHERE id = :id")
    suspend fun getCashItemById(id: Long): CashItemEntity?

    @Insert
    suspend fun insert(cashItem: CashItemEntity): Long

    @Update
    suspend fun update(cashItem: CashItemEntity)

    @Query("DELETE FROM cash_items WHERE id = :id")
    suspend fun deleteById(id: Long)
}
