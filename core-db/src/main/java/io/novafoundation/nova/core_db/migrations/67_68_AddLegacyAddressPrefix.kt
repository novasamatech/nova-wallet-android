package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddSellProviders_67_68 = object : Migration(67, 68) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chain_assets ADD COLUMN sellProviders TEXT")
    }
}
