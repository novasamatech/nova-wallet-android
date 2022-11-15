package io.novafoundation.nova.runtime.multiNetwork.chain

import com.google.gson.Gson
import io.novafoundation.nova.common.utils.CollectionDiffer
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainAssetToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainExplorersToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainNodeToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainToChainLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteAssetsToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteChainToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteExplorersToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteNodesToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainAssetRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainNodeRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainRemote
import io.novafoundation.nova.test_shared.argThat
import io.novafoundation.nova.test_shared.eq
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ChainSyncServiceTest {

    private val assetId = 0
    private val nodeUrl = "url"

    private val REMOTE_CHAIN = ChainRemote(
        chainId = "0x00",
        name = "Test",
        assets = listOf(
            ChainAssetRemote(
                assetId = assetId,
                symbol = "TEST",
                precision = 10,
                name = "Test",
                priceId = "test",
                staking = "test",
                type = null,
                typeExtras = null,
                icon = null,
                buyProviders = emptyMap()
            )
        ),
        nodes = listOf(
            ChainNodeRemote(
                url = nodeUrl,
                name = "test"
            )
        ),
        icon = "test",
        addressPrefix = 0,
        types = null,
        options = emptySet(),
        parentId = null,
        externalApi = null,
        explorers = emptyList(),
        additional = emptyMap()
    )

    private val gson = Gson()

    private val LOCAL_CHAIN = createLocalCopy(REMOTE_CHAIN)

    @Mock
    lateinit var dao: ChainDao

    @Mock
    lateinit var chainAssetDao: ChainAssetDao

    @Mock
    lateinit var chainFetcher: ChainFetcher

    lateinit var chainSyncService: ChainSyncService

    @Before
    fun setup() {
        chainSyncService = ChainSyncService(dao, chainFetcher, gson)
    }

    @Test
    fun `should insert new chain`() {
        runBlocking {
            localReturns(emptyList())
            localAssetReturn(listOf())
            remoteReturns(listOf(REMOTE_CHAIN))

            chainSyncService.syncUp()

            verify(dao).applyDiff(
                chainDiff = insertsChainWithId(REMOTE_CHAIN.chainId),
                assetsDiff = insertsAssetWithId(assetId),
                nodesDiff = insertsNodeWithUrl(nodeUrl),
                explorersDiff = emptyDiff()
            )
        }
    }

    @Test
    fun `should not insert the same chain`() {
        runBlocking {
            localReturns(listOf(LOCAL_CHAIN))
            localAssetReturn(LOCAL_CHAIN.assets)
            remoteReturns(listOf(REMOTE_CHAIN))

            chainSyncService.syncUp()

            verify(dao).applyDiff(emptyDiff(), emptyDiff(), emptyDiff(), emptyDiff())
        }
    }

    @Test
    fun `should update chain's own params`() {
        runBlocking {
            localReturns(listOf(LOCAL_CHAIN))
            localAssetReturn(LOCAL_CHAIN.assets)
            remoteReturns(listOf(REMOTE_CHAIN.copy(name = "new name")))

            chainSyncService.syncUp()

            verify(dao).applyDiff(
                chainDiff = insertsChainWithId(REMOTE_CHAIN.chainId),
                assetsDiff = emptyDiff(),
                nodesDiff = emptyDiff(),
                explorersDiff = emptyDiff(),
            )
        }
    }

    @Test
    fun `should update chain's asset`() {
        runBlocking {
            localReturns(listOf(LOCAL_CHAIN))
            localAssetReturn(LOCAL_CHAIN.assets)

            remoteReturns(listOf(REMOTE_CHAIN.copy(
                assets = listOf(
                    REMOTE_CHAIN.assets.first().copy(symbol = "NEW")
                )
            )))

            chainSyncService.syncUp()

            verify(dao).applyDiff(
                chainDiff = insertsChainWithId(REMOTE_CHAIN.chainId),
                assetsDiff = insertsAssetWithId(assetId),
                nodesDiff = emptyDiff(),
                explorersDiff = emptyDiff(),
            )
        }
    }

    @Test
    fun `should remove chain`() {
        runBlocking {
            localReturns(listOf(LOCAL_CHAIN))
            localAssetReturn(LOCAL_CHAIN.assets)

            remoteReturns(emptyList())

            chainSyncService.syncUp()

            verify(dao).applyDiff(
                chainDiff = removesChainWithId(REMOTE_CHAIN.chainId),
                assetsDiff = removesAssetWithId(assetId),
                nodesDiff = removesNodeWithUrl(nodeUrl),
                explorersDiff = emptyDiff()
            )
        }
    }

    private suspend fun remoteReturns(chains: List<ChainRemote>) {
        `when`(chainFetcher.getChains()).thenReturn(chains)
    }

    private suspend fun localAssetReturn(assets: List<ChainAssetLocal>) {
        `when`(chainAssetDao.getAssetsBySource(AssetSourceLocal.DEFAULT)).thenReturn(assets)
    }

    private suspend fun localReturns(chains: List<JoinedChainInfo>) {
        `when`(dao.getJoinChainInfo()).thenReturn(chains)
    }


    private fun insertsChainWithId(id: String) = insertsElement<ChainLocal> { it.id == id }
    private fun insertsAssetWithId(id: Int) = insertsElement<ChainAssetLocal> { it.id == id }
    private fun insertsNodeWithUrl(url: String) = insertsElement<ChainNodeLocal> { it.url == url }

    private fun <T> insertsElement(elementCheck: (T) -> Boolean) = argThat<CollectionDiffer.Diff<T>> {
        it.removed.isEmpty() && elementCheck(it.newOrUpdated.single())
    }

    private fun removesChainWithId(id: String) = removesElement<ChainLocal> { it.id == id }
    private fun removesAssetWithId(id: Int) = removesElement<ChainAssetLocal> { it.id == id }
    private fun removesNodeWithUrl(url: String) = removesElement<ChainNodeLocal> { it.url == url }

    private fun <T> removesElement(elementCheck: (T) -> Boolean) = argThat<CollectionDiffer.Diff<T>> {
        it.newOrUpdated.isEmpty() && elementCheck(it.removed.single())
    }

    private fun createLocalCopy(remote: ChainRemote): JoinedChainInfo {
        val domain = mapRemoteChainToLocal(remote, gson)
        val assets = mapRemoteAssetsToLocal(remote, gson)
        val nodes = mapRemoteNodesToLocal(remote)
        val explorers = mapRemoteExplorersToLocal(remote)

        return JoinedChainInfo(
            chain = domain,
            nodes = nodes,
            assets = assets,
            explorers = explorers
        )
    }

    private fun <T> emptyDiff() = eq(CollectionDiffer.Diff<T>(emptyList(), emptyList(), emptyList()))
}
