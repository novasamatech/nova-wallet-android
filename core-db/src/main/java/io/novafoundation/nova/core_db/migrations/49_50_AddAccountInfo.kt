package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddAccountInfo_49_50 = object : Migration(49, 50) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """CREATE TABLE IF NOT EXISTS `account_infos` (
            `chainId` TEXT NOT NULL, 
            `consumers` TEXT NOT NULL, 
            `data_free` TEXT NOT NULL, 
            `data_reserved` TEXT NOT NULL, 
            `data_frozen` TEXT NOT NULL, 
            PRIMARY KEY(`chainId`))""".trimMargin()
        )
    }
}
