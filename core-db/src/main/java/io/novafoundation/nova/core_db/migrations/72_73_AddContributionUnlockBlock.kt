package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddContributionUnlockBlock_72_73 = object: Migration(72, 73){

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM contributions")

        db.execSQL("ALTER TABLE contributions ADD COLUMN unlockBlock TEXT NOT NULL DEFAULT \'0\'")
    }
}
