package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal

val AddEnabledColumnToChainAssets_30_31 = object : Migration(30, 31) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `chain_assets` ADD COLUMN `enabled` INTEGER NOT NULL DEFAULT ${ChainAssetLocal.ENABLED_DEFAULT_STR}")
    }
}
