package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddBuyProviders_7_8 = object : Migration(7, 8) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chain_assets ADD COLUMN buyProviders TEXT DEFAULT null")
    }
}
