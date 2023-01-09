package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal

/**
 * Due to previous migration of chain & meta account tables by means of rename-create-insert-delete strategy
 * foreign keys to these tables got renamed and now points to wrong table which causes crashes for subset of users
 * This migration recreates all affected tables
 */
val FixBrokenForeignKeys_31_32 = object : Migration(31, 32) {

    override fun migrate(database: SupportSQLiteDatabase) {
        // foreign key to ChainLocal
        recreateChainAssets(database)

        // foreign key to ChainAssetLocal which was recreated above
        recreateAssets(database)

        // foreign key to MetaAccountLocal
        recreateChainAccount(database)

        // foreign key to ChainLocal
        recreateChainRuntimeInfo(database)

        // foreign key to ChainLocal
        recreateChainExplorers(database)

        // foreign key to ChainLocal
        recreateChainNodes(database)

        // foreign key to ChainLocal, ChainAssetLocal, MetaAccount
        recreateBalanceLocks(database)
    }

    private fun recreateChainAssets(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX IF EXISTS `index_chain_assets_chainId`")

        database.execSQL("ALTER TABLE chain_assets RENAME TO chain_assets_old")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chain_assets` (
            `id` INTEGER NOT NULL,
            `chainId` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `symbol` TEXT NOT NULL,
            `priceId` TEXT,
            `staking` TEXT NOT NULL,
            `precision` INTEGER NOT NULL,
            `icon` TEXT,
            `type` TEXT,
            `source` TEXT NOT NULL DEFAULT '${ChainAssetLocal.SOURCE_DEFAULT}',
            `buyProviders` TEXT,
            `typeExtras` TEXT,
            `enabled` INTEGER NOT NULL DEFAULT ${ChainAssetLocal.ENABLED_DEFAULT_STR},
            PRIMARY KEY(`chainId`,`id`),
            FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT INTO chain_assets
            SELECT 
                id, chainId, name, symbol, priceId,
                staking, precision, icon, type, source,
                buyProviders, typeExtras, enabled
            FROM chain_assets_old
            """.trimIndent()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_assets_chainId` ON `chain_assets` (`chainId`)")

        database.execSQL("DROP TABLE chain_assets_old")
    }

    private fun recreateAssets(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX IF EXISTS `index_assets_metaId`")
        database.execSQL("ALTER TABLE assets RENAME TO assets_old")

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
            FOREIGN KEY(`assetId`,`chainId`) REFERENCES `chain_assets`(`id`, `chainId`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT INTO assets
            SELECT 
                assetId, chainId, metaId,
                freeInPlanks, frozenInPlanks, reservedInPlanks,
                bondedInPlanks, redeemableInPlanks, unbondingInPlanks
            FROM assets_old
            """.trimIndent()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_metaId` ON `assets` (`metaId`)")

        database.execSQL("DROP TABLE assets_old")
    }

    private fun recreateChainAccount(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX IF EXISTS `index_chain_accounts_chainId`")
        database.execSQL("DROP INDEX IF EXISTS `index_chain_accounts_metaId`")
        database.execSQL("DROP INDEX IF EXISTS `index_chain_accounts_accountId`")

        database.execSQL("ALTER TABLE chain_accounts RENAME TO chain_accounts_old")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chain_accounts` (
            `metaId` INTEGER NOT NULL,
            `chainId` TEXT NOT NULL,
            `publicKey` BLOB,
            `accountId` BLOB NOT NULL,
            `cryptoType` TEXT,
            PRIMARY KEY(`metaId`, `chainId`),
            FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT INTO chain_accounts
            SELECT metaId, chainId, publicKey, accountId, cryptoType
            FROM chain_accounts_old
            """.trimIndent()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_accounts_chainId` ON `chain_accounts` (`chainId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_accounts_metaId` ON `chain_accounts` (`metaId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_accounts_accountId` ON `chain_accounts` (`accountId`)")

        database.execSQL("DROP TABLE chain_accounts_old")
    }

    private fun recreateChainRuntimeInfo(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX IF EXISTS `index_chain_runtimes_chainId`")

        database.execSQL("ALTER TABLE chain_runtimes RENAME TO chain_runtimes_old")

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

        database.execSQL(
            """
            INSERT INTO chain_runtimes
            SELECT chainId, syncedVersion, remoteVersion FROM chain_runtimes_old
            """.trimIndent()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_runtimes_chainId` ON `chain_runtimes` (`chainId`)")

        database.execSQL("DROP TABLE chain_runtimes_old")
    }

    private fun recreateChainExplorers(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX IF EXISTS `index_chain_explorers_chainId`")

        database.execSQL("ALTER TABLE chain_explorers RENAME TO chain_explorers_old")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chain_explorers` (
            `chainId` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `extrinsic` TEXT,
            `account` TEXT,
            `event` TEXT,
            PRIMARY KEY(`chainId`, `name`),
            FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT INTO chain_explorers
            SELECT chainId, name, extrinsic, account, event FROM chain_explorers_old
            """.trimIndent()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_explorers_chainId` ON `chain_explorers` (`chainId`)")

        database.execSQL("DROP TABLE chain_explorers_old")
    }

    private fun recreateChainNodes(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX IF EXISTS `index_chain_nodes_chainId`")

        database.execSQL("ALTER TABLE chain_nodes RENAME TO chain_nodes_old")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chain_nodes` (
            `chainId` TEXT NOT NULL,
            `url` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `orderId` INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(`chainId`, `url`),
            FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT INTO chain_nodes
            SELECT chainId, url, name, orderId FROM chain_nodes_old
            """.trimIndent()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_nodes_chainId` ON `chain_nodes` (`chainId`)")

        database.execSQL("DROP TABLE chain_nodes_old")
    }

    private fun recreateBalanceLocks(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE locks RENAME TO locks_old")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `locks` (
            `metaId` INTEGER NOT NULL,
            `chainId` TEXT NOT NULL,
            `assetId` INTEGER NOT NULL,
            `type` TEXT NOT NULL,
            `amount` TEXT NOT NULL,
            PRIMARY KEY(`metaId`, `chainId`, `assetId`, `type`), 
            FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
            FOREIGN KEY(`assetId`, `chainId`) REFERENCES `chain_assets`(`id`, `chainId`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT INTO locks
            SELECT metaId, chainId, assetId, type, amount FROM locks_old
            """.trimIndent()
        )

        database.execSQL("DROP TABLE locks_old")
    }
}
