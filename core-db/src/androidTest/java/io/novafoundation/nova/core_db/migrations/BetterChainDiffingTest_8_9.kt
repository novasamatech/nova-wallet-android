package io.novafoundation.nova.core_db.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import io.novafoundation.nova.core_db.AppDatabase
import io.novafoundation.nova.core_db.converters.CryptoTypeConverters
import io.novafoundation.nova.core_db.dao.assetOf
import io.novafoundation.nova.core_db.dao.chainOf
import io.novafoundation.nova.core_db.dao.testMetaAccount
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

private class OldAsset(
    val metaId: Long,
    val chainId: String,
    val tokenSymbol: String,
    val freeInPlanks: Int
)

class BetterChainDiffingTest_8_9 : BaseMigrationTest() {

    private val cryptoTypeConverters = CryptoTypeConverters()

    var meta1Id: Long = -1
    var meta2Id: Long = -1

    val chain1Id = "1"
    val chain2Id = "2"

    private lateinit var assetsOld: List<OldAsset>

    @Test
    fun validateMigration() = runMigrationTest(
        from = 8,
        to = 9,
        BetterChainDiffing_8_9,
        preMigrateBlock = ::preMigrate,
        postMigrateBlock = ::postMigrate
    )

    private fun preMigrate(db: SupportSQLiteDatabase) {
        db.beginTransaction()

        db.insertChain(chain1Id, assetSymbols = listOf("A", "B", "C"))
        db.insertChain(chain2Id, assetSymbols = listOf("C", "D", "E"))

        meta1Id = db.insertMetaAccount(name = "1")
        meta2Id = db.insertMetaAccount(name = "2")

        assetsOld = listOf(
            OldAsset(meta1Id, chain1Id, tokenSymbol = "A", freeInPlanks = 1),
            OldAsset(meta1Id, chain1Id, tokenSymbol = "B", freeInPlanks = 2),
            OldAsset(meta1Id, chain1Id, tokenSymbol = "C", freeInPlanks = 3),
            OldAsset(meta1Id, chain2Id, tokenSymbol = "C", freeInPlanks = 4),
            OldAsset(meta1Id, chain2Id, tokenSymbol = "D", freeInPlanks = 5),
            OldAsset(meta1Id, chain2Id, tokenSymbol = "E", freeInPlanks = 6),

            OldAsset(meta2Id, chain1Id, tokenSymbol = "A", freeInPlanks = 11),
            OldAsset(meta2Id, chain1Id, tokenSymbol = "C", freeInPlanks = 13),
        )

        assetsOld.forEach { db.insertAsset(it) }

        db.setTransactionSuccessful()
        db.endTransaction()
    }

    private suspend fun postMigrate(db: AppDatabase) {
        val assetsForMeta1 = db.assetDao().getSupportedAssets(meta1Id)

        val symbolToAssetIdMapping = mapOf(
            (chain1Id to "A") to 0,
            (chain1Id to "B") to 1,
            (chain1Id to "C") to 2,
            (chain2Id to "C") to 0,
            (chain2Id to "D") to 1,
            (chain2Id to "E") to 2,
        )

        assetsForMeta1.forEach {
            val actualChainId = it.asset!!.chainId
            val actualTokenSymbol = it.token!!.tokenSymbol

            val assetIdExpected = symbolToAssetIdMapping[actualChainId to actualTokenSymbol]
            assertEquals(assetIdExpected, it.asset!!.assetId)

            val expectedOldAsset = assetsOld.first { it.chainId == actualChainId && it.metaId == meta1Id && it.tokenSymbol == actualTokenSymbol }
            assertEquals(expectedOldAsset.freeInPlanks.toBigInteger(), it.asset!!.freeInPlanks)
        }
    }

    private fun SupportSQLiteDatabase.insertMetaAccount(
        name: String
    ): Long {
        val metaAccount = testMetaAccount(name)

        val contentValues = ContentValues().apply {
            put(MetaAccountLocal.Table.Column.SUBSTRATE_PUBKEY, metaAccount.substratePublicKey)
            put(MetaAccountLocal.Table.Column.SUBSTRATE_ACCOUNT_ID, metaAccount.substrateAccountId)
            put(MetaAccountLocal.Table.Column.ETHEREUM_ADDRESS, metaAccount.ethereumAddress)
            put(MetaAccountLocal.Table.Column.ETHEREUM_PUBKEY, metaAccount.ethereumPublicKey)
            put(MetaAccountLocal.Table.Column.NAME, metaAccount.name)
            put(MetaAccountLocal.Table.Column.SUBSTRATE_CRYPTO_TYPE, cryptoTypeConverters.from(metaAccount.substrateCryptoType))
            put(MetaAccountLocal.Table.Column.IS_SELECTED, metaAccount.isSelected)
            put(MetaAccountLocal.Table.Column.POSITION, metaAccount.position)
        }

        return insert(MetaAccountLocal.TABLE_NAME, 0, contentValues)
    }

    private fun SupportSQLiteDatabase.insertChain(
        id: String,
        assetSymbols: List<String>
    ) {
        val chain = chainOf(id)

        val contentValues = ContentValues().apply {
            put("parentId", chain.parentId)
            put("name", chain.name)
            put("additional", chain.additional)
            put("id", chain.id)
            put("icon", chain.icon)
            // types
            putNull("url")
            putNull("overridesCommon")
            // externalApi
            putNull("staking_url")
            putNull("staking_type")
            putNull("history_type")
            putNull("history_url")
            putNull("crowdloans_url")
            putNull("crowdloans_type")

            put("prefix", chain.prefix)
            put("isEthereumBased", chain.isEthereumBased)
            put("isTestNet", chain.isTestNet)
            put("hasCrowdloans", chain.hasCrowdloans)
        }

        insert("chains", 0, contentValues)

        val assets = assetSymbols.mapIndexed { index, symbol ->
            chain.assetOf(assetId = index, symbol)
        }

        assets.forEach {
            val contentValues = ContentValues().apply {
                put("id", it.id)
                put("chainId", it.chainId)
                put("name", it.name)
                put("symbol", it.symbol)
                put("priceId", it.priceId)
                put("staking", it.staking)
                put("precision", it.precision)
                put("icon", it.icon)
                put("type", it.type)
                put("typeExtras", it.typeExtras)
                put("buyProviders", it.buyProviders)
            }

            insert("chain_assets", 0, contentValues)
        }
    }

    private fun SupportSQLiteDatabase.insertAsset(oldAsset: OldAsset) {
        val tokenContentValues = ContentValues().apply {
            put("symbol", oldAsset.tokenSymbol)
            putNull("dollarRate")
            putNull("recentRateChange")
        }

        insert("tokens", SQLiteDatabase.CONFLICT_REPLACE, tokenContentValues)

        val assetContentValues = ContentValues().apply {
            put("tokenSymbol", oldAsset.tokenSymbol)
            put("chainId", oldAsset.chainId)
            put("metaId", oldAsset.metaId)

            val amountZero = BigInteger.ZERO.toString()

            put("freeInPlanks", oldAsset.freeInPlanks.toString())
            put("frozenInPlanks", amountZero)
            put("reservedInPlanks", amountZero)

            put("bondedInPlanks", amountZero)
            put("redeemableInPlanks", amountZero)
            put("unbondingInPlanks", amountZero)
        }

        insert("assets", 0, assetContentValues)
    }
}
