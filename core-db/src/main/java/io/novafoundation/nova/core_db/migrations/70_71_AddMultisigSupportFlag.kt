package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val AddMultisigSupportFlag_70_71 = object : Migration(70, 71) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add multisigSupport column to chains table
        db.execSQL("ALTER TABLE `chains` ADD COLUMN `multisigSupport` INTEGER NOT NULL DEFAULT 0")

        // Update multisigSupport flag based on the presence of MULTISIG external API
        db.execSQL(
            """
            UPDATE chains SET multisigSupport = 1
            WHERE id IN (
                SELECT DISTINCT chainId FROM chain_external_apis 
                WHERE apiType = 'MULTISIG'
            )
            """.trimIndent()
        )

        // Delete all multisig external apis
        db.execSQL(
            """
                DELETE FROM chain_external_apis 
                WHERE apiType = 'MULTISIG'
            """.trimIndent()
        )
    }
}
