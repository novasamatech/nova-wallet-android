package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.model.chain.ChainLocal

val AddRuntimeFlagToChains_36_37 = object : Migration(36, 37) {

    override fun migrate(database: SupportSQLiteDatabase) {
        val default = ChainLocal.Default.HAS_SUBSTRATE_RUNTIME

        database.execSQL("ALTER TABLE chains ADD COLUMN `hasSubstrateRuntime` INTEGER NOT NULL DEFAULT $default")
    }
}
