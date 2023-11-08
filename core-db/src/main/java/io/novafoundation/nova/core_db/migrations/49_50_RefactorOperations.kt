@file:Suppress("ktlint")

package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val RefactorOperations_49_50 = object : Migration(49, 50) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE operations")

        database.execSQL("CREATE TABLE IF NOT EXISTS `operations` (`id` TEXT NOT NULL, `address` TEXT NOT NULL, `time` INTEGER NOT NULL, `status` INTEGER NOT NULL, `source` INTEGER NOT NULL, `hash` TEXT, `chainId` TEXT NOT NULL, `assetId` INTEGER NOT NULL, PRIMARY KEY(`id`, `address`, `chainId`, `assetId`))")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_operations_hash` ON `operations` (`hash`)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `operation_transfers` (`amount` TEXT NOT NULL, `sender` TEXT NOT NULL, `receiver` TEXT NOT NULL, `fee` TEXT, `operationId` TEXT NOT NULL, `address` TEXT NOT NULL, `chainId` TEXT NOT NULL, `assetId` INTEGER NOT NULL, PRIMARY KEY(`operationId`, `address`, `chainId`, `assetId`), FOREIGN KEY(`operationId`, `address`, `chainId`, `assetId`) REFERENCES `operations`(`id`, `address`, `chainId`, `assetId`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_operation_transfers_operationId_address_chainId_assetId` ON `operation_transfers` (`operationId`, `address`, `chainId`, `assetId`)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `operation_rewards_direct` (`isReward` INTEGER NOT NULL, `amount` TEXT NOT NULL, `eventId` TEXT NOT NULL, `era` INTEGER, `validator` TEXT, `operationId` TEXT NOT NULL, `address` TEXT NOT NULL, `chainId` TEXT NOT NULL, `assetId` INTEGER NOT NULL, PRIMARY KEY(`operationId`, `address`, `chainId`, `assetId`), FOREIGN KEY(`operationId`, `address`, `chainId`, `assetId`) REFERENCES `operations`(`id`, `address`, `chainId`, `assetId`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_operation_rewards_direct_operationId_address_chainId_assetId` ON `operation_rewards_direct` (`operationId`, `address`, `chainId`, `assetId`)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `operation_rewards_pool` (`isReward` INTEGER NOT NULL, `amount` TEXT NOT NULL, `eventId` TEXT NOT NULL, `poolId` INTEGER NOT NULL, `operationId` TEXT NOT NULL, `address` TEXT NOT NULL, `chainId` TEXT NOT NULL, `assetId` INTEGER NOT NULL, PRIMARY KEY(`operationId`, `address`, `chainId`, `assetId`), FOREIGN KEY(`operationId`, `address`, `chainId`, `assetId`) REFERENCES `operations`(`id`, `address`, `chainId`, `assetId`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_operation_rewards_pool_operationId_address_chainId_assetId` ON `operation_rewards_pool` (`operationId`, `address`, `chainId`, `assetId`)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `operation_extrinsics` (`contentType` TEXT NOT NULL, `module` TEXT NOT NULL, `call` TEXT, `fee` TEXT NOT NULL, `operationId` TEXT NOT NULL, `address` TEXT NOT NULL, `chainId` TEXT NOT NULL, `assetId` INTEGER NOT NULL, PRIMARY KEY(`operationId`, `address`, `chainId`, `assetId`), FOREIGN KEY(`operationId`, `address`, `chainId`, `assetId`) REFERENCES `operations`(`id`, `address`, `chainId`, `assetId`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_operation_extrinsics_operationId_address_chainId_assetId` ON `operation_extrinsics` (`operationId`, `address`, `chainId`, `assetId`)")
        database.execSQL("CREATE TABLE IF NOT EXISTS `operation_swaps` (`operationId` TEXT NOT NULL, `address` TEXT NOT NULL, `chainId` TEXT NOT NULL, `assetId` INTEGER NOT NULL, `fee_amount` TEXT NOT NULL, `fee_chainId` TEXT NOT NULL, `fee_assetId` INTEGER NOT NULL, `assetIn_amount` TEXT NOT NULL, `assetIn_chainId` TEXT NOT NULL, `assetIn_assetId` INTEGER NOT NULL, `assetOut_amount` TEXT NOT NULL, `assetOut_chainId` TEXT NOT NULL, `assetOut_assetId` INTEGER NOT NULL, PRIMARY KEY(`operationId`, `address`, `chainId`, `assetId`), FOREIGN KEY(`operationId`, `address`, `chainId`, `assetId`) REFERENCES `operations`(`id`, `address`, `chainId`, `assetId`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_operation_swaps_operationId_address_chainId_assetId` ON `operation_swaps` (`operationId`, `address`, `chainId`, `assetId`)")
    }
}
