package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddBrowserTabs_64_65 = object : Migration(64, 65) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `browser_tabs` (
                `id` TEXT NOT NULL, 
                `metaId` INTEGER NOT NULL, 
                `currentUrl` TEXT NOT NULL, 
                `creationTime` INTEGER NOT NULL, 
                `pageName` TEXT, `pageIconPath` TEXT, 
                `pagePicturePath` TEXT, 
                `dappMetadata_iconLink` TEXT,
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """
        )
    }
}
