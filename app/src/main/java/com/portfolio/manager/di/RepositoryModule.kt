package com.portfolio.manager.di

import com.portfolio.manager.data.local.AccountDao
import com.portfolio.manager.data.local.CashItemDao
import com.portfolio.manager.data.local.HoldingDao
import com.portfolio.manager.data.local.PriceHistoryDao
import com.portfolio.manager.data.local.StockNameDao
import com.portfolio.manager.data.remote.YahooFinanceApi
import com.portfolio.manager.data.repository.AccountRepositoryImpl
import com.portfolio.manager.data.repository.CashRepositoryImpl
import com.portfolio.manager.data.repository.HoldingsRepositoryImpl
import com.portfolio.manager.data.repository.StockRepositoryImpl
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.CashRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.StockRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideHoldingsRepository(holdingDao: HoldingDao): HoldingsRepository {
        return HoldingsRepositoryImpl(holdingDao)
    }

    @Provides
    @Singleton
    fun provideAccountRepository(accountDao: AccountDao): AccountRepository {
        return AccountRepositoryImpl(accountDao)
    }

    @Provides
    @Singleton
    fun provideStockRepository(
        api: YahooFinanceApi,
        priceHistoryDao: PriceHistoryDao,
        stockNameDao: StockNameDao
    ): StockRepository {
        return StockRepositoryImpl(api, priceHistoryDao, stockNameDao)
    }

    @Provides
    @Singleton
    fun provideCashRepository(cashItemDao: CashItemDao): CashRepository {
        return CashRepositoryImpl(cashItemDao)
    }
}
