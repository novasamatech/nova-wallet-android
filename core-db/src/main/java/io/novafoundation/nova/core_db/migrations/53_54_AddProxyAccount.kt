package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.converters.AssetConverters
import io.novafoundation.nova.core_db.model.AssetLocal

val AddProxyAccount_53_54 = object : Migration(53, 54) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `proxy_accounts` (
            `metaId` INTEGER NOT NULL, 
            `chainId` TEXT NOT NULL, 
            `delegatorAccountId` BLOB NOT NULL, 
            `rightType` TEXT NOT NULL, 
            `status` TEXT NOT NULL, 
            PRIMARY KEY(`metaId`, `delegatorAccountId`, `chainId`, `rightType`), 
            FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimMargin()
        )
    }
}
