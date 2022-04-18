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

    private var metaId: Long = 0

    private val chainId = "0"
    private val testChain = createTestChain(chainId)
    private val assetId = testChain.assets.first().id

    @Before
    fun setupDb() = runBlocking {
        chainDao.addChain(testChain)
        metaId = metaAccountDao.insertMetaAccount(testMetaAccount())
    }

    @Test
    fun shouldDeleteAssetAfterChainIsDeleted() = runBlocking {
        dao.insertAsset(AssetLocal.createEmpty(assetId = assetId, chainId = chainId, metaId))

        chainDao.removeChain(testChain)

        val assets = dao.getAssets(metaId)

        assert(assets.isEmpty())
    }
}
