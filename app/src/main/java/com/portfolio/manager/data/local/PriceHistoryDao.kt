package com.portfolio.manager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PriceHistoryDao {
    @Query("SELECT * FROM price_history WHERE symbol = :symbol AND range = :range")
    suspend fun getPriceHistory(symbol: String, range: String): PriceHistoryEntity?

    @Query("SELECT * FROM price_history WHERE symbol IN (:symbols) AND range = :range")
    suspend fun getPriceHistoryForSymbols(symbols: List<String>, range: String): List<PriceHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistory(priceHistory: PriceHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceHistories(priceHistories: List<PriceHistoryEntity>)

    @Query("DELETE FROM price_history WHERE symbol = :symbol")
    suspend fun deletePriceHistoryForSymbol(symbol: String)

    @Query("DELETE FROM price_history WHERE symbol NOT IN (:symbols)")
    suspend fun deletePriceHistoryNotIn(symbols: List<String>)

    @Query("DELETE FROM price_history")
    suspend fun deleteAll()
}
