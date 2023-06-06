package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddWalletConnectSessions_39_40 = object : Migration(39, 40) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wallet_connect_sessions` (
            `sessionTopic` TEXT NOT NULL,
            `metaId` INTEGER NOT NULL,
            PRIMARY KEY(`sessionTopic`),
            FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        database.execSQL("CREATE INDEX IF NOT EXISTS `index_wallet_connect_sessions_metaId` ON `wallet_connect_sessions` (`metaId`)")
    }
}
