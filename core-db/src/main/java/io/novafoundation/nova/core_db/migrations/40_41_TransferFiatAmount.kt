package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val TransferFiatAmount_40_41 = object : Migration(40, 41) {
    override fun migrate(database: SupportSQLiteDatabase) {
        createCoinPriceTable(database)
    }

    private fun createCoinPriceTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `coin_prices` (
            `priceId` TEXT NOT NULL, 
            `currencyId` TEXT NOT NULL, 
            `timestamp` INTEGER NOT NULL, 
            `rate` TEXT NOT NULL,
            PRIMARY KEY(`priceId`, `currencyId`, `timestamp`)
            )
            """.trimIndent()
        )
    }
}
