package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddCurrencies_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
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
    }
}
