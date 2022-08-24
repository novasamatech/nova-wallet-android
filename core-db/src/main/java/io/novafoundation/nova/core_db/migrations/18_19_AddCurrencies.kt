package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddCurrencies_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        addCurrenciesTable(database)
        recreateTokensTable(database)
    }

    private fun addCurrenciesTable(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        // new table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `currencies` (
            `code` TEXT NOT NULL, 
            `name` TEXT NOT NULL, 
            `symbol` TEXT, 
            `category` TEXT NOT NULL, 
            `popular` INTEGER NOT NULL, 
            `id` INTEGER NOT NULL, 
            `coingeckoId` TEXT NOT NULL, 
            `selected` INTEGER NOT NULL, 
            PRIMARY KEY(`id`)
        )
            """.trimIndent()
        )

        database.setTransactionSuccessful()
        database.endTransaction()
    }

    private fun recreateTokensTable(database: SupportSQLiteDatabase) {
        // rename
        database.execSQL("ALTER TABLE tokens RENAME TO tokens_old")

        // new table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `tokens` (
            `symbol` TEXT NOT NULL, 
            `rate` TEXT, 
            `recentRateChange` TEXT, 
            `currencyId` INTEGER NOT NULL DEFAULT(0), 
            PRIMARY KEY(`symbol`)
            )
            """.trimIndent())

        // insert to new from old
        database.execSQL(
            """
            INSERT INTO chain_accounts (symbol, rate, recentRateChange)
            SELECT symbol, dollarRate, recentRateChange
            FROM chain_accounts_old
            """.trimIndent()
        )

        // delete old
        database.execSQL("DROP TABLE tokens_old")
    }
}
