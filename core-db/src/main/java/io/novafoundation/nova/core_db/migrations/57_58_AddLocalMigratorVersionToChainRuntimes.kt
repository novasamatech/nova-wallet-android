package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddLocalMigratorVersionToChainRuntimes_57_58 = object : Migration(57, 58) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `chain_runtimes` ADD COLUMN `localMigratorVersion` INTEGER NOT NULL DEFAULT 1")
    }
}
