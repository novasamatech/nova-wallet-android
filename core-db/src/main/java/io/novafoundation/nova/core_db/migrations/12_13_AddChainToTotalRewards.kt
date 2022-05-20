package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddChainToTotalRewards_12_13 = object : Migration(12, 13) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE total_reward")

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `total_reward` (
                `accountAddress` TEXT NOT NULL,
                `chainId` TEXT NOT NULL,
                `chainAssetId` INTEGER NOT NULL,
                `totalReward` TEXT NOT NULL,
                PRIMARY KEY(`chainId`, `chainAssetId`, `accountAddress`)
            )
        """.trimIndent())
    }
}
