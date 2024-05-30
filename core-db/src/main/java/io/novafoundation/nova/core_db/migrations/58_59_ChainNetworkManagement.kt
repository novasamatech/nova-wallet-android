package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.model.chain.ChainLocal

val ChainNetworkManagement_58_59 = object : Migration(58, 59) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chains ADD COLUMN `isCustomNetwork` INTEGER NOT NULL DEFAULT ${ChainLocal.DEFAULT_NETWORK_BY_DEFAULT_STR}")
    }
}
