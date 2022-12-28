package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val ExtractExternalApiToSeparateTable_34_35 = object : Migration(34, 35) {

    override fun migrate(database: SupportSQLiteDatabase) {
        recreateChains(database)

        // recreating chainId causes broken foreign keys to appear on some devices
        // so we run this migration again to fix it
        FixBrokenForeignKeys_31_32.migrate(database)
    }

    private fun recreateChains(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chains RENAME TO chains_old")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chains` (
            `id` TEXT NOT NULL,
            `parentId` TEXT,
            `name` TEXT NOT NULL,
            `icon` TEXT NOT NULL,
            `prefix` INTEGER NOT NULL,
            `isEthereumBased` INTEGER NOT NULL,
            `isTestNet` INTEGER NOT NULL,
            `hasCrowdloans` INTEGER NOT NULL,
            `governance` TEXT NOT NULL,
            `additional` TEXT,
            `url` TEXT,
            `overridesCommon` INTEGER,
             PRIMARY KEY(`id`)
            ) 
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT INTO chains
            SELECT 
                id, parentId, name, icon, prefix,
                isEthereumBased, isTestNet, hasCrowdloans, governance, additional,
                url, overridesCommon
            FROM chains_old
            """.trimIndent()
        )

        database.execSQL("DROP TABLE chains_old")
    }
}
