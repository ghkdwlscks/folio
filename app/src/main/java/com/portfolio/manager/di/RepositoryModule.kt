package com.portfolio.manager.di

import com.portfolio.manager.data.local.AccountDao
import com.portfolio.manager.data.local.HoldingDao
import com.portfolio.manager.data.remote.YahooFinanceApi
import com.portfolio.manager.data.repository.AccountRepositoryImpl
import com.portfolio.manager.data.repository.HoldingsRepositoryImpl
import com.portfolio.manager.data.repository.StockRepositoryImpl
import com.portfolio.manager.domain.repository.AccountRepository
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
    fun provideStockRepository(api: YahooFinanceApi): StockRepository {
        return StockRepositoryImpl(api)
    }
}
