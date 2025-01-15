package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddFavoriteDAppsOrdering_65_66 = object : Migration(65, 66) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE favourite_dapps ADD COLUMN orderingIndex INTEGER NOT NULL DEFAULT 0")
    }
}
