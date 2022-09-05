package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val ChangeChainNodes_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chain_nodes ADD `orderId` INTEGER NOT NULL DEFAULT 0")
    }
}
