package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AssetTypes_2_3 = object : Migration(2, 3) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chain_assets ADD COLUMN type TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE chain_assets ADD COLUMN typeExtras TEXT DEFAULT NULL")
        database.execSQL("ALTER TABLE chain_assets ADD COLUMN icon TEXT DEFAULT NULL")
    }
}
