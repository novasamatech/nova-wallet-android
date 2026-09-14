package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val PerWalletTokenState_74_75 = object : Migration(74, 75) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chain_asset_visibility` (
            `metaId` INTEGER NOT NULL,
            `chainId` TEXT NOT NULL,
            `assetId` INTEGER NOT NULL,
            `visible` INTEGER NOT NULL,
            PRIMARY KEY(`metaId`, `chainId`, `assetId`),
            FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE ,
            FOREIGN KEY(`assetId`, `chainId`) REFERENCES `chain_assets`(`id`, `chainId`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_asset_visibility_metaId` ON `chain_asset_visibility` (`metaId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chain_asset_visibility_assetId_chainId` ON `chain_asset_visibility` (`assetId`, `chainId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `meta_account_settings` (
            `metaId` INTEGER NOT NULL,
            `autoAddTokensWithBalance` INTEGER NOT NULL,
            PRIMARY KEY(`metaId`),
            FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """.trimIndent()
        )

        // Hiding a token used to be a per-install flag, so every wallet inherits those decisions.
        // Has to happen before the first sync: balance discovery only skips assets that already
        // have a row, so a token the user hid while still holding a balance in it would come back.
        db.execSQL(
            """
            INSERT OR REPLACE INTO `chain_asset_visibility` (`metaId`, `chainId`, `assetId`, `visible`)
            SELECT m.`id`, a.`chainId`, a.`id`, 0 FROM `chain_assets` AS a CROSS JOIN `meta_accounts` AS m
            WHERE a.`enabled` = 0
            """.trimIndent()
        )

        // Everything else a wallet could see before the update stays visible, zero balances included.
        // The curated default list is for wallets that start fresh; imposing it on an existing one would
        // silently take away tokens its owner is used to seeing. Only wallets that exist right now get
        // these rows - a wallet created after the update starts from the curated list like a new install.
        db.execSQL(
            """
            INSERT OR IGNORE INTO `chain_asset_visibility` (`metaId`, `chainId`, `assetId`, `visible`)
            SELECT m.`id`, a.`chainId`, a.`id`, 1 FROM `chain_assets` AS a CROSS JOIN `meta_accounts` AS m
            WHERE a.`enabled` = 1
            """.trimIndent()
        )
    }
}
