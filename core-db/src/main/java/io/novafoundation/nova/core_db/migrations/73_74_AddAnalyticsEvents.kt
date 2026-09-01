package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddAnalyticsEvents_73_74 = object : Migration(73, 74) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `analytics_pending_events` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `name` TEXT NOT NULL,
            `timestamp` INTEGER NOT NULL,
            `propsJson` TEXT NOT NULL);
            """.trimIndent()
        )
    }
}
