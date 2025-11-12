package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddFieldsToContributions = object: Migration(72, 73){

    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE contributions")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `contributions` (
            `metaId` INTEGER NOT NULL,
             `chainId` TEXT NOT NULL, 
             `assetId` INTEGER NOT NULL,
             `paraId` TEXT NOT NULL, 
             `amountInPlanks` TEXT NOT NULL, 
             `sourceId` TEXT NOT NULL,
             `unlockBlock` TEXT NOT NULL,
             `leaseDepositor` BLOB NOT NULL,
             PRIMARY KEY(`metaId`, `chainId`, `assetId`, `paraId`, `sourceId`))");
        """.trimIndent())
    }
}
