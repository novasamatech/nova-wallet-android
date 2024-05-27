package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddLocalMigratorVersionToChainRuntimes_58_59 = object : Migration(58, 59) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `chain_runtimes` ADD COLUMN `localMigratorVersion` INTEGER NOT NULL DEFAULT 1")
    }
}
