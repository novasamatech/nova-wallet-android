package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddChainColor_4_5 = object : Migration(4, 5) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chains ADD COLUMN color TEXT DEFAULT NULL")
    }
}
