package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddProxyAccount_54_55 = object : Migration(54, 55) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `proxy_accounts` (
            `proxiedMetaId` INTEGER NOT NULL, 
            `proxyMetaId` INTEGER NOT NULL, 
            `chainId` TEXT NOT NULL, 
            `proxiedAccountId` BLOB NOT NULL, 
            `proxyType` TEXT NOT NULL, 
            PRIMARY KEY(`proxyMetaId`, `proxiedAccountId`, `chainId`, `proxyType`), 
            FOREIGN KEY(`proxiedMetaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
            FOREIGN KEY(`proxyMetaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )

        database.execSQL("ALTER TABLE `meta_accounts` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'ACTIVE'")
        database.execSQL("ALTER TABLE `meta_accounts` ADD COLUMN `parentMetaId` INTEGER")
        database.execSQL("ALTER TABLE `chains` ADD COLUMN `supportProxy` INTEGER NOT NULL DEFAULT 0")
    }
}
