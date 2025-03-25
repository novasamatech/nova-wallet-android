package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddLegacyAddressPrefix_66_67 = object : Migration(66, 67) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chains ADD COLUMN legacyPrefix INTEGER")
    }
}
