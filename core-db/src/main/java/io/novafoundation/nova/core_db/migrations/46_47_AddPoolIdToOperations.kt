package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddPoolIdToOperations_46_47 = object : Migration(46, 47) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE operations ADD COLUMN poolId INTEGER")
    }
}
