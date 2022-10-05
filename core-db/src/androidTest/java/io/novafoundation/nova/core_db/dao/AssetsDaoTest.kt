package io.novafoundation.nova.core_db.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.novafoundation.nova.core_db.AppDatabase
import io.novafoundation.nova.core_db.model.AssetLocal
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetsDaoTest : DaoTest<AssetDao>(AppDatabase::assetDao) {

    private val chainDao by dao<ChainDao>()
    private val metaAccountDao by dao<MetaAccountDao>()
    private val currencyDao by dao<CurrencyDao>()
    private val assetDao by dao<AssetDao>()

    private var metaId: Long = 0

    private val chainId = "0"
    private val testChain = createTestChain(chainId)
    private val asset = testChain.assets.first()
    private val assetId = asset.id

    @Before
    fun setupDb() = runBlocking {
        metaId = metaAccountDao.insertMetaAccount(testMetaAccount())
        chainDao.addChain(testChain)
    }

    @Test
    fun shouldDeleteAssetAfterChainIsDeleted() = runBlocking {
        dao.insertAsset(AssetLocal.createEmpty(assetId = assetId, chainId = chainId, metaId))
        chainDao.removeChain(testChain)

        val assets = dao.getSupportedAssets(metaId)

        assert(assets.isEmpty())
    }

    @Test
    fun testRetrievingAssetsByMetaId() = runBlocking {
        currencyDao.insert(createCurrency(selected = true))

        val assetWithToken = dao.getAssetWithToken(metaId, chainId, assetId)

        assert(assetWithToken != null)
    }

    @Test
    fun testRetrievingAssetsByMetaIdWithoutCurrency() = runBlocking {
        currencyDao.insert(createCurrency(selected = false))

        val assetWithToken = dao.getAssetWithToken(metaId, chainId, assetId)

        assert(assetWithToken == null)
    }

    @Test
    fun testRetrievingSyncedAssets() = runBlocking {
        assetDao.insertAsset(AssetLocal.createEmpty(assetId, chainId, metaId))
        currencyDao.insert(createCurrency(selected = true))

        val assetWithToken = dao.getSyncedAssets(metaId)

        assert(assetWithToken.isNotEmpty())
    }

    @Test
    fun testRetrievingSyncedAssetsWithoutCurrency() = runBlocking {
        assetDao.insertAsset(AssetLocal.createEmpty(assetId, chainId, metaId))
        currencyDao.insert(createCurrency(selected = false))

        val assetsWithTokens = dao.getSyncedAssets(metaId)

        assert(assetsWithTokens.isEmpty())
    }

    @Test
    fun testRetrievingSyncedAssetsWithoutAssetBalance() = runBlocking {
        currencyDao.insert(createCurrency(selected = false))

        val assetsWithTokens = dao.getSyncedAssets(metaId)

        assert(assetsWithTokens.isEmpty())
    }

    @Test
    fun testRetrievingSupportedAssets() = runBlocking {
        currencyDao.insert(createCurrency(selected = true))

        val assetsWithTokens = dao.getSupportedAssets(metaId)

        assert(assetsWithTokens.isNotEmpty())
    }

    @Test
    fun testRetrievingSupportedAssetsWithoutCurrency() = runBlocking {
        currencyDao.insert(createCurrency(selected = false))

        val assetsWithTokens = dao.getSupportedAssets(metaId)

        assert(assetsWithTokens.isEmpty())
    }
}
