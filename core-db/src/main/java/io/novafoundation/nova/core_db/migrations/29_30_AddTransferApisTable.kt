package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddTransferApisTable_29_30 = object : Migration(29, 30) {
    override fun migrate(database: SupportSQLiteDatabase) {
        removeTransferApiFieldsFromChains(database)

        addTransferApiTable(database)

        clearOperationsCache(database)
    }

    private fun clearOperationsCache(database: SupportSQLiteDatabase) {
        database.execSQL("DELETE FROM operations")
    }

    private fun addTransferApiTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chain_transfer_history_apis` (
                `chainId` TEXT NOT NULL,
                `assetType` TEXT NOT NULL,
                `apiType` TEXT NOT NULL,
                `url` TEXT NOT NULL,
                PRIMARY KEY(`chainId`, `url`),
                FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_chain_transfer_history_apis_chainId` ON `chain_transfer_history_apis` (`chainId`)
            """.trimIndent()
        )
    }

    private fun removeTransferApiFieldsFromChains(database: SupportSQLiteDatabase) {
        // rename
        database.execSQL("ALTER TABLE chains RENAME TO chains_old")

        // new table
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
            `staking_url` TEXT,
            `staking_type` TEXT,
            `crowdloans_url` TEXT,
            `crowdloans_type` TEXT,
            `governance_url` TEXT,
            `governance_type` TEXT,
             PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        // insert to new from old
        database.execSQL(
            // select all but color
            """
            INSERT INTO chains
            SELECT id, parentId, name, icon, prefix, isEthereumBased, isTestNet, hasCrowdloans, governance,
            additional, url, overridesCommon, staking_url, staking_type, crowdloans_url, crowdloans_type, governance_url, governance_type
            FROM chains_old
            """.trimIndent()
        )

        // delete old
        database.execSQL("DROP TABLE chains_old")
    }
}
