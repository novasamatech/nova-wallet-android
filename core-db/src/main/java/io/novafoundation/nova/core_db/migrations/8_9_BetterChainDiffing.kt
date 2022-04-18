package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val BetterChainDiffing_8_9 = object : Migration(8, 9) {

    override fun migrate(database: SupportSQLiteDatabase) {
        migrateAssets(database)

        migrateRuntimeVersions(database)
    }

    private fun migrateRuntimeVersions(database: SupportSQLiteDatabase) {
        // rename
        database.execSQL("DROP INDEX `index_chain_runtimes_chainId`")
        database.execSQL("ALTER TABLE chain_runtimes RENAME TO chain_runtimes_old")

        // new table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chain_runtimes` (
            `chainId` TEXT NOT NULL,
            `syncedVersion` INTEGER NOT NULL,
            `remoteVersion` INTEGER NOT NULL,
            PRIMARY KEY(`chainId`),
            FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX `index_chain_runtimes_chainId` ON `chain_runtimes` (`chainId`)")

        // insert to new from old
        database.execSQL(
            """
            INSERT INTO chain_runtimes
            SELECT chainId, syncedVersion, remoteVersion
            FROM chain_runtimes_old
            """.trimIndent()
        )

        // delete old
        database.execSQL("DROP TABLE chain_runtimes_old")
    }

    private fun migrateAssets(database: SupportSQLiteDatabase) {
        // rename
        database.execSQL("DROP INDEX `index_assets_metaId`")
        database.execSQL("ALTER TABLE assets RENAME TO assets_old")

        // new table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `assets` (
            `assetId` INTEGER NOT NULL,
            `chainId` TEXT NOT NULL,
            `metaId` INTEGER NOT NULL,
            `freeInPlanks` TEXT NOT NULL,
            `frozenInPlanks` TEXT NOT NULL,
            `reservedInPlanks` TEXT NOT NULL,
            `bondedInPlanks` TEXT NOT NULL,
            `redeemableInPlanks` TEXT NOT NULL,
            `unbondingInPlanks` TEXT NOT NULL,
            PRIMARY KEY(`assetId`,`chainId`,`metaId`),
            FOREIGN KEY(`assetId`, `chainId`) REFERENCES `chain_assets`(`id`, `chainId`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_metaId` ON `assets` (`metaId`)".trimIndent())

        // insert to new from old
        database.execSQL(
            """
            INSERT INTO assets
            SELECT
            ca.id, ca.chainId,
            a.metaId, a.freeInPlanks, a.frozenInPlanks, a.reservedInPlanks, a.bondedInPlanks, a.redeemableInPlanks, a.unbondingInPlanks
            FROM assets_old AS a INNER JOIN chain_assets AS ca WHERE a.tokenSymbol = ca.symbol AND a.chainId = ca.chainId
            """.trimIndent()
        )

        // delete old
        database.execSQL("DROP TABLE assets_old")
    }
}
