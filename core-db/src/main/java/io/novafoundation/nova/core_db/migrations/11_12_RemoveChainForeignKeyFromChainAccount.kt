package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val RemoveChainForeignKeyFromChainAccount_11_12 = object : Migration(11, 12) {

    override fun migrate(database: SupportSQLiteDatabase) {
        // rename
        database.execSQL("DROP INDEX `index_chain_accounts_chainId`")
        database.execSQL("DROP INDEX `index_chain_accounts_metaId`")
        database.execSQL("DROP INDEX `index_chain_accounts_accountId`")
        database.execSQL("ALTER TABLE chain_accounts RENAME TO chain_accounts_old")

        // new table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chain_accounts` (
            `metaId` INTEGER NOT NULL,
            `chainId` TEXT NOT NULL,
            `publicKey` BLOB NOT NULL,
            `accountId` BLOB NOT NULL,
            `cryptoType` TEXT NOT NULL,
            PRIMARY KEY(`metaId`, `chainId`),
            FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_accounts_chainId` ON `chain_accounts` (`chainId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_accounts_metaId` ON `chain_accounts` (`metaId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_accounts_accountId` ON `chain_accounts` (`accountId`)")

        // insert to new from old
        database.execSQL(
            """
            INSERT INTO chain_accounts
            SELECT *
            FROM chain_accounts_old
            """.trimIndent()
        )

        // delete old
        database.execSQL("DROP TABLE chain_accounts_old")
    }
}
