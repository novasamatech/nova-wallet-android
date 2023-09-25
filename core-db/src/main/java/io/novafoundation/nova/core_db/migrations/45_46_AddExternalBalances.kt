package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddExternalBalances_45_46 = object : Migration(45, 46) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `externalBalances` (
            `metaId` INTEGER NOT NULL,
            `chainId` TEXT NOT NULL,
            `assetId` INTEGER NOT NULL,
            `type` TEXT NOT NULL,
            `subtype` TEXT NOT NULL,
            `amount` TEXT NOT NULL,
            PRIMARY KEY(`metaId`, `chainId`, `assetId`, `type`, `subtype`),
            FOREIGN KEY(`assetId`, `chainId`) REFERENCES `chain_assets`(`id`, `chainId`) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """
        )
    }
}
