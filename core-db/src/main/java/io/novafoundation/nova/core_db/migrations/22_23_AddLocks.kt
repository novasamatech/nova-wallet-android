package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddLocks_22_23 = object : Migration(22, 23) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // new table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `locks` (
                `metaId` INTEGER NOT NULL, 
                `chainId` TEXT NOT NULL, 
                `assetId` INTEGER NOT NULL, 
                `type` TEXT NOT NULL, 
                `amount` TEXT NOT NULL, 
                PRIMARY KEY(`metaId`, `chainId`, `assetId`, `type`), 
                FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`assetId`, `chainId`) REFERENCES `chain_assets`(`id`, `chainId`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )
    }
}
