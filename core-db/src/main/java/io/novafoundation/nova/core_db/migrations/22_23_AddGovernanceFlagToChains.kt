package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddGovernanceFlagToChains_22_23 = object : Migration(22, 23) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chains ADD COLUMN hasGovernance INTEGER NOT NULL DEFAULT 0")
    }
}
