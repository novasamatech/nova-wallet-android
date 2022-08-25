package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val ChangeTokens_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // rename table
        database.execSQL("ALTER TABLE tokens RENAME TO tokens_old")

        // new table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tokens` (
            `tokenSymbol` TEXT NOT NULL, 
            `rate` TEXT, 
            `recentRateChange` TEXT, 
            `currencyId` INTEGER NOT NULL, 
            PRIMARY KEY(`tokenSymbol`, `currencyId`)
            )
            """.trimIndent()
        )

        // insert to new from old
        database.execSQL(
            """
            INSERT INTO tokens (tokenSymbol, rate, recentRateChange, currencyId)
            SELECT symbol, dollarRate, recentRateChange, 0
            FROM tokens_old
            """.trimIndent()
        )

        // delete old
        database.execSQL("DROP TABLE tokens_old")
    }
}
