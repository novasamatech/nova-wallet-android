package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val ChainNetworkManagement_61_62 = object : Migration(61, 62) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chains ADD COLUMN `customFee` TEXT NOT NULL DEFAULT ''")
    }
}
