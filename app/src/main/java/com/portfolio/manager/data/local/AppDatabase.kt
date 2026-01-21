package com.portfolio.manager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AccountEntity::class, HoldingEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun holdingDao(): HoldingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create accounts table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)

                // Insert default account
                db.execSQL("""
                    INSERT INTO accounts (id, name, createdAt) VALUES (1, 'Default', ${System.currentTimeMillis()})
                """)

                // Create new holdings table with accountId
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS holdings_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        accountId INTEGER NOT NULL,
                        symbol TEXT NOT NULL,
                        name TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        averagePrice REAL NOT NULL,
                        currency TEXT NOT NULL DEFAULT 'USD',
                        FOREIGN KEY (accountId) REFERENCES accounts(id) ON DELETE CASCADE
                    )
                """)

                // Copy existing holdings to new table with default accountId
                db.execSQL("""
                    INSERT INTO holdings_new (id, accountId, symbol, name, quantity, averagePrice, currency)
                    SELECT id, 1, symbol, name, quantity, averagePrice, currency FROM holdings
                """)

                // Drop old holdings table
                db.execSQL("DROP TABLE holdings")

                // Rename new table to holdings
                db.execSQL("ALTER TABLE holdings_new RENAME TO holdings")

                // Create index on accountId
                db.execSQL("CREATE INDEX IF NOT EXISTS index_holdings_accountId ON holdings(accountId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add orderIndex column to accounts table
                db.execSQL("ALTER TABLE accounts ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
                // Set initial order based on id
                db.execSQL("UPDATE accounts SET orderIndex = id")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add preferredCurrency column to accounts table
                db.execSQL("ALTER TABLE accounts ADD COLUMN preferredCurrency TEXT NOT NULL DEFAULT 'USD'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "portfolio_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
