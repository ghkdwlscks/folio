package com.portfolio.manager.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StockNameDao {
    @Query("SELECT * FROM stock_names WHERE symbol = :symbol")
    suspend fun getStockName(symbol: String): StockNameEntity?

    @Query("SELECT * FROM stock_names WHERE symbol IN (:symbols)")
    suspend fun getStockNames(symbols: List<String>): List<StockNameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockName(stockName: StockNameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockNames(stockNames: List<StockNameEntity>)
}
