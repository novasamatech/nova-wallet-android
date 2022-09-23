package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddContributions_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `contributions` (
                `metaId` INTEGER NOT NULL, 
                `chainId` TEXT NOT NULL, 
                `assetId` INTEGER NOT NULL, 
                `paraId` TEXT NOT NULL, 
                `amountInPlanks` TEXT NOT NULL, 
                `sourceId` TEXT NOT NULL, 
                PRIMARY KEY(`metaId`, `chainId`, `assetId`, `paraId`, `sourceId`)
            )
            """.trimIndent()
        )
    }
}
