package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val NftTagsAndAttributes_48_49 = object : Migration(48, 49) {

    override fun migrate(database: SupportSQLiteDatabase) {
        createCoinPriceTable(database)
    }

    private fun createCoinPriceTable(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            ALTER TABLE nfts 
            ADD COLUMN tags TEXT;
            """.trimIndent()
        )
        database.execSQL(
            """
            ALTER TABLE nfts 
            ADD COLUMN attributes TEXT;
            """.trimIndent()
        )
    }
}
