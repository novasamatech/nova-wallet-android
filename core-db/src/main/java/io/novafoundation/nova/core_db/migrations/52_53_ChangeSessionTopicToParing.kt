package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val ChangeSessionTopicToParing_52_53 = object : Migration(52, 53) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE wallet_connect_sessions")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wallet_connect_pairings` (
            `pairingTopic` TEXT NOT NULL,
            `metaId` INTEGER NOT NULL,
            PRIMARY KEY(`pairingTopic`),
            FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
            """
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_connect_pairings_metaId` ON `wallet_connect_pairings` (`metaId`)")
    }
}
