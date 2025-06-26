package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddMultisigCalls_69_70 = object : Migration(69, 70) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `multisig_operation_call` (
                `metaId` INTEGER NOT NULL,
                `chainId` TEXT NOT NULL, 
                `callHash` TEXT NOT NULL, 
                `callInstance` TEXT NOT NULL, 
                PRIMARY KEY(`metaId`, `chainId`, `callHash`), 
                FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
    }
}
