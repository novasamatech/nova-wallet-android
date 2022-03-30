package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddSitePhishing_6_7 = object : Migration(6, 7) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `phishing_sites` (`host` TEXT NOT NULL, PRIMARY KEY(`host`))")
    }
}
