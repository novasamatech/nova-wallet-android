package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddVersioningToGovernanceDapps_32_33 = object : Migration(32, 33) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE `governance_dapps`")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `governance_dapps` (
                `chainId` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                `referendumUrlV1` TEXT, 
                `referendumUrlV2` TEXT, 
                `iconUrl` TEXT NOT NULL, 
                `details` TEXT NOT NULL, 
                PRIMARY KEY(`chainId`, `name`)
            )
            """.trimIndent()
        )
    }
}
