package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.model.chain.ChainLocal

val AddConnectionStateToChains_52_53 = object : Migration(52, 53) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val defaultConnectionState = ChainLocal.Default.CONNECTION_STATE_DEFAULT

        database.execSQL("ALTER TABLE chains ADD COLUMN connectionState TEXT NOT NULL DEFAULT '${defaultConnectionState}'")

        // Enable full for chains that have some balance synced
        database.execSQL("""
            UPDATE chains SET connectionState = 'FULL_SYNC'
            WHERE id IN (
                SELECT DISTINCT chainId
                FROM assets
                WHERE freeInPlanks > 0 OR frozenInPlanks > 0 OR reservedInPlanks > 0
            ) """.trimIndent())
    }
}
