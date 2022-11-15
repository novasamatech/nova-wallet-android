package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddSourceToLocalAsset_28_29 = object : Migration(27, 28) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `chain_assets` ADD COLUMN `source` TEXT NOT NULL")
        database.execSQL("UPDATE `chain_assets` SET `source`='DEFAULT'")
    }
}
