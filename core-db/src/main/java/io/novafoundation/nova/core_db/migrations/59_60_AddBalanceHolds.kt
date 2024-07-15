package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddBalanceHolds_59_60 = object : Migration(59, 60) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `holds` (
                `metaId` INTEGER NOT NULL, 
                `chainId` TEXT NOT NULL, 
                `assetId` INTEGER NOT NULL,
                `amount` TEXT NOT NULL, 
                `id_module` TEXT NOT NULL,
                `id_reason` TEXT NOT NULL,
                PRIMARY KEY(`metaId`, `chainId`, `assetId`, `id_module`, `id_reason`), 
                FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                FOREIGN KEY(`assetId`, `chainId`) REFERENCES `chain_assets`(`id`, `chainId`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )
    }
}
