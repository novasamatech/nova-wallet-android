package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val StakingRewardPeriods_42_43 = object : Migration(42, 43) {

    override fun migrate(database: SupportSQLiteDatabase) {
        createCoinPriceTable(database)
    }

    private fun createCoinPriceTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `staking_reward_period` (
            `accountId` BLOB NOT NULL, 
            `chainId` TEXT NOT NULL, 
            `assetId` INTEGER NOT NULL, 
            `stakingType` TEXT NOT NULL, 
            `periodType` TEXT NOT NULL, 
            `customPeriodStart` INTEGER, 
            `customPeriodEnd` INTEGER, 
            `offsetFromCurrentDate` INTEGER, 
            PRIMARY KEY(`accountId`, `chainId`, `assetId`, `stakingType`))
            """.trimIndent()
        )
    }
}
