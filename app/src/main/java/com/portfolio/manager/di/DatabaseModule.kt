package com.portfolio.manager.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.portfolio.manager.data.local.AccountDao
import com.portfolio.manager.data.local.AppDatabase
import com.portfolio.manager.data.local.CashItemDao
import com.portfolio.manager.data.local.HoldingDao
import com.portfolio.manager.data.local.PriceHistoryDao
import com.portfolio.manager.data.local.StockNameDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS cash_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    accountId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    originalValue REAL NOT NULL,
                    annualYieldRate REAL NOT NULL,
                    currency TEXT NOT NULL DEFAULT 'USD',
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY (accountId) REFERENCES accounts(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_cash_items_accountId ON cash_items(accountId)")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "portfolio_database"
        )
            .addMigrations(MIGRATION_10_11)
            .fallbackToDestructiveMigration()
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

    @Provides
    fun provideStockNameDao(database: AppDatabase): StockNameDao {
        return database.stockNameDao()
    }

    @Provides
    fun provideCashItemDao(database: AppDatabase): CashItemDao {
        return database.cashItemDao()
    }
}
