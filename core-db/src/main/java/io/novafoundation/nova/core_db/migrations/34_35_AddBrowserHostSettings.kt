package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddBrowserHostSettings_34_35 = object : Migration(34, 35) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `browser_host_settings` (
                `hostUrl` TEXT NOT NULL, 
                `isDesktopModeEnabled` INTEGER NOT NULL, 
                PRIMARY KEY(`hostUrl`)
            )
            """.trimIndent()
        )
    }
}
