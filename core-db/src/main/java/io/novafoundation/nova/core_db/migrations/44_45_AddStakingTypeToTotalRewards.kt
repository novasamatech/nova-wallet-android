package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


val AddStakingTypeToTotalRewards_44_45 = object : Migration(44, 45) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE total_reward")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `total_reward` (
                `accountId` BLOB NOT NULL,
                `chainId` TEXT NOT NULL,
                `chainAssetId` INTEGER NOT NULL,
                `stakingType` TEXT NOT NULL,
                `totalReward` TEXT NOT NULL,
                PRIMARY KEY(`chainId`,`chainAssetId`,`stakingType`, `accountId`)
            )
            """.trimIndent()
        )
    }
}
