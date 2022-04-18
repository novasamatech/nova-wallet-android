package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddFavouriteDApps_9_10 = object : Migration(9, 10) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `favourite_dapps` (
            `url` TEXT NOT NULL,
            `label` TEXT NOT NULL,
            `icon` TEXT, PRIMARY KEY(`url`)
            )
            """.trimIndent()
        )
    }
}
