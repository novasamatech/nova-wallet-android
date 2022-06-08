package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// used on master for Astar hotfix
val AddAdditionalFieldToChains_12_13 = object : Migration(12, 13) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chains ADD COLUMN additional TEXT DEFAULT null")
    }
}

// used on develop for parachainStaking rewards
val AddChainToTotalRewards_12_13 = object : Migration(12, 13) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE total_reward")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `total_reward` (
                `accountAddress` TEXT NOT NULL,
                `chainId` TEXT NOT NULL,
                `chainAssetId` INTEGER NOT NULL,
                `totalReward` TEXT NOT NULL,
                PRIMARY KEY(`chainId`, `chainAssetId`, `accountAddress`)
            )
            """.trimIndent()
        )
    }
}

val FixMigrationConflicts_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        if (isMigratingFromMaster(database)) {
            // migrating from master -> execute missing develop migration
            AddChainToTotalRewards_12_13.migrate(database)
        } else {
            // migrating from develop -> execute missing master migration
            AddAdditionalFieldToChains_12_13.migrate(database)
        }
    }

    private fun isMigratingFromMaster(database: SupportSQLiteDatabase): Boolean {
        return runCatching {
            // check for column added in astar hotfix (master)
            database.query("SELECT additional FROM chains LIMIT 1")
        }.fold(
            onSuccess = { true },
            onFailure = { false }
        )
    }
}
