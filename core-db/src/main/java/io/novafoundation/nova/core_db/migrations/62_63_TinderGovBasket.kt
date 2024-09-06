package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val TinderGovBasket_62_63 = object : Migration(62, 63) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tinder_gov_basket` (
                `referendumId` TEXT NOT NULL, 
                `metaId` INTEGER NOT NULL, 
                `chainId` TEXT NOT NULL, 
                `amount` TEXT NOT NULL,
                `conviction` TEXT NOT NULL, 
                `voteType` TEXT NOT NULL,
                PRIMARY KEY(`referendumId`, `metaId`, `chainId`)
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tinder_gov_basket` (
                `metaId` INTEGER NOT NULL, 
                `chainId` TEXT NOT NULL, 
                `amount` TEXT NOT NULL,
                `conviction` TEXT NOT NULL, 
                PRIMARY KEY(`metaId`, `chainId`)
            )
            """.trimIndent()
        )
    }
}
