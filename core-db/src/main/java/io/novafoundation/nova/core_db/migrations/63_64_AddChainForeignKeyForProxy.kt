package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddChainForeignKeyForProxy_63_64 = object : Migration(63, 64) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `proxy_accounts_new` (
                `proxiedMetaId` INTEGER NOT NULL, 
                `proxyMetaId` INTEGER NOT NULL, 
                `chainId` TEXT NOT NULL, 
                `proxiedAccountId` BLOB NOT NULL, 
                `proxyType` TEXT NOT NULL, 
                PRIMARY KEY(`proxyMetaId`, `proxiedAccountId`, `chainId`, `proxyType`), 
                FOREIGN KEY(`proxiedMetaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`proxyMetaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """
        )

        database.execSQL(
            """
            INSERT INTO `proxy_accounts_new` (`proxiedMetaId`, `proxyMetaId`, `chainId`, `proxiedAccountId`, `proxyType`)
            SELECT `proxiedMetaId`, `proxyMetaId`, `chainId`, `proxiedAccountId`, `proxyType` 
            FROM `proxy_accounts`
        """
        )

        database.execSQL("DROP TABLE `proxy_accounts`")

        database.execSQL("ALTER TABLE `proxy_accounts_new` RENAME TO `proxy_accounts`")
    }
}
