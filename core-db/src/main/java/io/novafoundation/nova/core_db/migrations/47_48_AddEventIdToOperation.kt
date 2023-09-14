package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddEventIdToOperation_47_48 = object : Migration(47, 48) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE operations ADD COLUMN eventId TEXT")

        database.execSQL("DELETE FROM operations")
    }
}
