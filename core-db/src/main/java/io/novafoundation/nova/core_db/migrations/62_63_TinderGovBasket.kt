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
                PRIMARY KEY(`referendumId`, `metaId`, `chainId`),
                FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tinder_gov_voting_power` (
                `metaId` INTEGER NOT NULL, 
                `chainId` TEXT NOT NULL, 
                `amount` TEXT NOT NULL,
                `conviction` TEXT NOT NULL, 
                PRIMARY KEY(`metaId`, `chainId`),
                FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}
