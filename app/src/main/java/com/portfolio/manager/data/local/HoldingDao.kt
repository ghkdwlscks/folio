package com.portfolio.manager.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldingDao {
    @Query("SELECT * FROM holdings WHERE accountId = :accountId ORDER BY symbol ASC")
    fun getHoldingsByAccount(accountId: Long): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings ORDER BY symbol ASC")
    fun getAllHoldings(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE id = :id")
    suspend fun getHoldingById(id: Long): HoldingEntity?

    @Query("SELECT * FROM holdings WHERE accountId = :accountId AND symbol = :symbol")
    suspend fun getHoldingByAccountAndSymbol(accountId: Long, symbol: String): HoldingEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(holding: HoldingEntity): Long

    @Update
    suspend fun update(holding: HoldingEntity)

    @Delete
    suspend fun delete(holding: HoldingEntity)

    @Query("DELETE FROM holdings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM holdings WHERE accountId = :accountId")
    suspend fun getHoldingsCountByAccount(accountId: Long): Int

    @Query("SELECT accountId, COUNT(*) as count FROM holdings GROUP BY accountId")
    fun getHoldingsCountByAccountFlow(): Flow<List<AccountHoldingCount>>

    @Query("UPDATE holdings SET targetPercentage = :targetPercentage WHERE id = :holdingId")
    suspend fun updateTargetPercentage(holdingId: Long, targetPercentage: Int?)

    @Query("SELECT * FROM holdings WHERE accountId = :accountId")
    suspend fun getHoldingsByAccountSync(accountId: Long): List<HoldingEntity>
}

data class AccountHoldingCount(
    val accountId: Long,
    val count: Int
)
