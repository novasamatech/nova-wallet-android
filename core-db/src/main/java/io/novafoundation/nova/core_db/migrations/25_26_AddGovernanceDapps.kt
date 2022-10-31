package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddGovernanceDapps_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // new table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `governance_dapps` (
                `chainId` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                `referendumUrl` TEXT NOT NULL, 
                `iconUrl` TEXT NOT NULL, 
                `details` TEXT NOT NULL, 
                PRIMARY KEY(`chainId`, `name`)
            )
            """.trimIndent()
        )
    }
}
