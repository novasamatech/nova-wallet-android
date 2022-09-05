package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val ChangeChainNodes_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // rename table
        database.execSQL("ALTER TABLE chain_nodes RENAME TO chain_nodes_old")

        // new table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chain_nodes` (
                `chainId` TEXT NOT NULL,
                `url` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                `orderId` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`chainId`, `url`), 
                FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """.trimIndent()
        )

        // insert to new from old
        database.execSQL(
            """
            INSERT INTO chain_nodes (chainId, url, name, orderId)
            SELECT chainId, url, name, 0
            FROM chain_nodes_old
            """.trimIndent()
        )

        // delete old
        database.execSQL("DROP TABLE chain_nodes_old")
    }
}
