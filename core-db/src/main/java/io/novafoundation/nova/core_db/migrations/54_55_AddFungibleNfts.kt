package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddFungibleNfts_54_55 = object : Migration(54, 55) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX IF EXISTS `index_nfts_metaId`")
        database.execSQL("DROP TABLE nfts")

        database.execSQL("CREATE TABLE IF NOT EXISTS `nfts` (`identifier` TEXT NOT NULL, `metaId` INTEGER NOT NULL, `chainId` TEXT NOT NULL, `collectionId` TEXT NOT NULL, `instanceId` TEXT, `metadata` BLOB, `type` TEXT NOT NULL, `wholeDetailsLoaded` INTEGER NOT NULL, `name` TEXT, `label` TEXT, `media` TEXT, `issuanceType` TEXT NOT NULL, `issuanceTotal` TEXT, `issuanceMyEdition` TEXT, `issuanceMyAmount` TEXT, `price` TEXT, `pricedUnits` TEXT, PRIMARY KEY(`identifier`))")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_nfts_metaId` ON `nfts` (`metaId`)")
    }
}
