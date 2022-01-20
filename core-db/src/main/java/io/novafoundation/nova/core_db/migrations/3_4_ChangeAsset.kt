package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val ChangeAsset_3_4 = object : Migration(3, 4) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE assets")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `assets` (
                `tokenSymbol` TEXT NOT NULL,
                `chainId` TEXT NOT NULL,
                `metaId` INTEGER NOT NULL,
                `freeInPlanks` TEXT NOT NULL,
                `frozenInPlanks` TEXT NOT NULL,
                `reservedInPlanks` TEXT NOT NULL,
                `bondedInPlanks` TEXT NOT NULL,
                `redeemableInPlanks` TEXT NOT NULL,
                `unbondingInPlanks` TEXT NOT NULL,
                PRIMARY KEY(`tokenSymbol`, `chainId`, `metaId`)
            )
            """.trimIndent()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_assets_metaId` ON `assets` (`metaId`)")
    }
}
