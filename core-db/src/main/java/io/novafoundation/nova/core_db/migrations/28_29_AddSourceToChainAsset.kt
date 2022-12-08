package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal

val AddSourceToLocalAsset_28_29 = object : Migration(28, 29) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `chain_assets` ADD COLUMN `source` TEXT NOT NULL DEFAULT '${ChainAssetLocal.SOURCE_DEFAULT}'")
    }
}
