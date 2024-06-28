package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.NodeSelectionPreferencesLocal

val ChainNetworkManagement_58_59 = object : Migration(58, 59) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chains ADD COLUMN source TEXT NOT NULL DEFAULT '${ChainLocal.DEFAULT_NETWORK_SOURCE_STR}'")
        database.execSQL("ALTER TABLE chain_nodes ADD COLUMN source TEXT NOT NULL DEFAULT '${ChainNodeLocal.DEFAULT_NODE_SOURCE_STR}'")

        // Create node preferences table
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `node_selection_preferences` (
            `chainId` TEXT NOT NULL, 
            `autoBalanceEnabled` INTEGER NOT NULL DEFAULT ${NodeSelectionPreferencesLocal.DEFAULT_AUTO_BALANCE_DEFAULT_STR}, 
            `selectedNodeUrl` TEXT, PRIMARY KEY(`chainId`), 
            FOREIGN KEY(`chainId`) REFERENCES `chains`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Fill new table with default values
        database.execSQL(
            """
            INSERT INTO node_selection_preferences (chainId, autobalanceEnabled, selectedNodeUrl)
            SELECT id, ${NodeSelectionPreferencesLocal.DEFAULT_AUTO_BALANCE_DEFAULT_STR}, NULL FROM chain
            """.trimIndent()
        )
    }
}
