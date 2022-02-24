package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddNfts_5_6 = object : Migration(5, 6) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `nfts` (
            `identifier` TEXT NOT NULL,
            `metaId` INTEGER NOT NULL,
            `chainId` TEXT NOT NULL,
            `collectionId` TEXT,
            `instanceId` TEXT,
            `metadata` BLOB,
            `name` TEXT,
            `label` TEXT,
            `media` TEXT,
            `price` TEXT,
            `type` TEXT NOT NULL,
            PRIMARY KEY(`identifier`))
            """
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_nfts_metaId` ON `nfts` (`metaId`)")
    }
}
