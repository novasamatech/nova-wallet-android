package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddWalletFavourites_73_74 = object : Migration(73, 74) {

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meta_accounts ADD COLUMN isFavourite INTEGER NOT NULL DEFAULT 0")
    }
}
