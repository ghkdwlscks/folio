package com.portfolio.manager.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.portfolio.manager.data.local.AccountDao
import com.portfolio.manager.data.local.AppDatabase
import com.portfolio.manager.data.local.HoldingDao
import com.portfolio.manager.data.local.PriceHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "portfolio_database"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("portfolio_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    fun provideHoldingDao(database: AppDatabase): HoldingDao {
        return database.holdingDao()
    }

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    fun providePriceHistoryDao(database: AppDatabase): PriceHistoryDao {
        return database.priceHistoryDao()
    }
}
