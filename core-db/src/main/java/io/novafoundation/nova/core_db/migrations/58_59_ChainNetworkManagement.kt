package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal

val ChainNetworkManagement_58_59 = object : Migration(58, 59) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE chains ADD COLUMN `isCustomNetwork` INTEGER NOT NULL DEFAULT ${ChainLocal.DEFAULT_NETWORK_BY_DEFAULT_STR}")
        database.execSQL("ALTER TABLE chains ADD COLUMN `autoBalanceEnabled` INTEGER NOT NULL DEFAULT ${ChainLocal.DEFAULT_AUTO_BALANCE_DEFAULT_STR}")
        database.execSQL("ALTER TABLE chains ADD COLUMN `defaultNodeId` TEXT")

        database.execSQL("ALTER TABLE chain_nodes ADD COLUMN `isCustom` INTEGER NOT NULL DEFAULT ${ChainNodeLocal.DEFAULT_IS_CUSTOM_NODE_STR}")
    }
}
