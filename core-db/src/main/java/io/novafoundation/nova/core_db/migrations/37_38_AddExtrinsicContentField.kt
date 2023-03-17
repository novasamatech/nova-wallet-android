package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddExtrinsicContentField_37_38 = object : Migration(37, 38) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE operations")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `operations` (
            `id` TEXT NOT NULL,
            `address` TEXT NOT NULL,
            `chainId` TEXT NOT NULL,
            `chainAssetId` INTEGER NOT NULL,
            `time` INTEGER NOT NULL,
            `status` INTEGER NOT NULL,
            `source` INTEGER NOT NULL,
            `operationType` INTEGER NOT NULL,
            `amount` TEXT,
            `sender` TEXT,
            `receiver` TEXT,
            `hash` TEXT,
            `fee` TEXT,
            `isReward` INTEGER,
            `era` INTEGER,
            `validator` TEXT,
            `extrinsicContent_type` TEXT,
            `extrinsicContent_module` TEXT,
            `extrinsicContent_call` TEXT,
            PRIMARY KEY(`id`, `address`, `chainId`, `chainAssetId`)
            )
            """.trimIndent()
        )
    }
}
