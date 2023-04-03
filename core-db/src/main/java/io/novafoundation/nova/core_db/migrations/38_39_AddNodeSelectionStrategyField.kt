package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.model.chain.ChainLocal

val AddNodeSelectionStrategyField_38_39 = object : Migration(38, 39) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val default = ChainLocal.Default.NODE_SELECTION_STRATEGY_DEFAULT

        database.execSQL("ALTER TABLE chains ADD COLUMN `nodeSelectionStrategy` TEXT NOT NULL DEFAULT '$default'")
    }
}
