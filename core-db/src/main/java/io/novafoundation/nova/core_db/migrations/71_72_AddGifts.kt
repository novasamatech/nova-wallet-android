package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddGifts_71_72 = object : Migration(71, 72) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `gifts` (
                    `amount` TEXT NOT NULL, 
                    `chainId` TEXT NOT NULL, 
                    `assetId` INTEGER NOT NULL,
                    `status` TEXT NOT NULL,
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    FOREIGN KEY(`assetId`, `chainId`) REFERENCES `chain_assets`(`id`, `chainId`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )
    }
}
