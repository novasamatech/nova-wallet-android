package io.novafoundation.nova.core_db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.converters.ExternalBalanceTypeConverters
import io.novafoundation.nova.core_db.model.ExternalBalanceLocal

val AddExternalBalances_45_46 = object : Migration(45, 46) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `externalBalances` (
            `metaId` INTEGER NOT NULL,
            `chainId` TEXT NOT NULL,
            `assetId` INTEGER NOT NULL,
            `type` TEXT NOT NULL,
            `subtype` TEXT NOT NULL,
            `amount` TEXT NOT NULL,
            PRIMARY KEY(`metaId`, `chainId`, `assetId`, `type`, `subtype`),
            FOREIGN KEY(`assetId`, `chainId`) REFERENCES `chain_assets`(`id`, `chainId`) ON UPDATE NO ACTION ON DELETE CASCADE,
            FOREIGN KEY(`metaId`) REFERENCES `meta_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
            """
        )

//        fillExternalBalancesWithContributionsData(database)
    }

    private fun fillExternalBalancesWithContributionsData(database: SupportSQLiteDatabase) {
        val converters = ExternalBalanceTypeConverters()
        val crowdloanId = converters.fromType(ExternalBalanceLocal.Type.CROWDLOAN)

        database.execSQL(
            """
            INSERT INTO externalBalances
            SELECT metaId, chainId, assetId,
            "$crowdloanId" as type, sourceId as subtype, SUM(amountInPlanks) as amount 
            FROM contributions
            WHERE SELECT EXISTS
            GROUP BY metaId, chainId, assetId, sourceId
            """.trimIndent()
        )
    }
}
