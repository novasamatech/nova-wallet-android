package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddProxyAccount_53_54 = object : Migration(53, 54) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `proxy_accounts` (
            `metaId` INTEGER NOT NULL, 
            `chainId` TEXT NOT NULL, 
            `proxiedAccountId` BLOB NOT NULL, 
            `proxyType` TEXT NOT NULL, 
            `status` TEXT NOT NULL, 
            PRIMARY KEY(`metaId`, `proxiedAccountId`, `chainId`, `proxyType`), 
            FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )

        database.execSQL("ALTER TABLE 'chains' ADD COLUMN `supportProxy` INTEGER NOT NULL DEFAULT 0")
    }
}
