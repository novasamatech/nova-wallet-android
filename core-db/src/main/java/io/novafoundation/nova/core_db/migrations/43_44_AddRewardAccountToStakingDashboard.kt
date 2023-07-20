package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddRewardAccountToStakingDashboard_43_44 = object : Migration(43, 44) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE `staking_dashboard_items`")
        database.execSQL("DROP INDEX IF EXISTS `index_staking_dashboard_items_metaId`")

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
                `stakeStatusAccount` BLOB,
                `rewardsAccount` BLOB,
                PRIMARY KEY(`chainId`, `chainAssetId`, `stakingType`, `metaId`),
                FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`chainAssetId`, `chainId`) REFERENCES `chain_assets`(`id`, `chainId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_staking_dashboard_items_metaId` ON `staking_dashboard_items` (`metaId`)")
    }
}
