package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddStakingDashboardItems_41_42 = object : Migration(41, 42) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `staking_dashboard_items` (
                `chainId` TEXT NOT NULL,
                `chainAssetId` INTEGER NOT NULL,
                `stakingType` TEXT NOT NULL,
                `metaId` INTEGER NOT NULL,
                `hasStake` INTEGER NOT NULL,
                `stake` TEXT,
                `status` TEXT,
                `rewards` TEXT,
                `estimatedEarnings` REAL,
                `primaryStakingAccountId` BLOB,
                PRIMARY KEY(`chainId`, `chainAssetId`, `stakingType`, `metaId`),
                FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`chainAssetId`, `chainId`) REFERENCES `chain_assets`(`id`, `chainId`) ON UPDATE NO ACTION ON DELETE CASCADE ,
                FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_staking_dashboard_items_metaId` ON `staking_dashboard_items` (`metaId`)")
    }
}
