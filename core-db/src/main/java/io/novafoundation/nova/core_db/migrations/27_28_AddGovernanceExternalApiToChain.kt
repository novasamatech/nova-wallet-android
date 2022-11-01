package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddGovernanceExternalApiToChain_27_28 = object : Migration(27, 28) {

    override fun migrate(database: SupportSQLiteDatabase) {
        // new columns
        database.execSQL("ALTER TABLE `chains` ADD COLUMN `governance_url` TEXT")
        database.execSQL("ALTER TABLE `chains` ADD COLUMN `governance_type` TEXT")
    }
}
