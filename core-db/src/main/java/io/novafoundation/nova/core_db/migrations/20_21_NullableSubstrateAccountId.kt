package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val NullableSubstrateAccountId_21_22 = object : Migration(21, 22) {

    override fun migrate(database: SupportSQLiteDatabase) {
        // rename
        database.execSQL("DROP INDEX `index_meta_accounts_substrateAccountId`")
        database.execSQL("DROP INDEX `index_meta_accounts_ethereumAddress`")
        database.execSQL("ALTER TABLE meta_accounts RENAME TO meta_accounts_old")

        // new table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `meta_accounts` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `substratePublicKey` BLOB,
                `substrateCryptoType` TEXT,
                `substrateAccountId` BLOB,
                `ethereumPublicKey` BLOB,
                `ethereumAddress` BLOB,
                `name` TEXT NOT NULL,
                `isSelected` INTEGER NOT NULL,
                `position` INTEGER NOT NULL,
                `type` TEXT NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_meta_accounts_substrateAccountId` ON `meta_accounts` (`substrateAccountId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_meta_accounts_ethereumAddress` ON `meta_accounts` (`ethereumAddress`)")

        // insert to new from old
        database.execSQL(
            """
            INSERT INTO meta_accounts
            SELECT *
            FROM meta_accounts_old
            """.trimIndent()
        )

        // delete old
        database.execSQL("DROP TABLE meta_accounts_old")
    }
}
