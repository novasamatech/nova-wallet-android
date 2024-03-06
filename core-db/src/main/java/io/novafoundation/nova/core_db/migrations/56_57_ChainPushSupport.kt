package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val ChainPushSupport_56_57 = object : Migration(56, 57) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `chains` ADD COLUMN `pushSupport` INTEGER NOT NULL DEFAULT 0")
    }
}
