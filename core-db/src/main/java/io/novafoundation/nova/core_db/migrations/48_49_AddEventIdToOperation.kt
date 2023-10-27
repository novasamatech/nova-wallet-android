package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddSwapOption_48_49 = object : Migration(48, 49) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chains ADD COLUMN `swap` TEXT NOT NULL DEFAULT ''")
    }
}
