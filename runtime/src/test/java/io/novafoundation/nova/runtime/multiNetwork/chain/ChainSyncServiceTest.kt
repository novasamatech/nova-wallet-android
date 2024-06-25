package io.novafoundation.nova.runtime.multiNetwork.chain

import com.google.gson.Gson
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExplorerLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.JoinedChainInfo
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapExternalApisToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteAssetToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteChainToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteExplorersToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapRemoteNodesToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.ChainFetcher
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainAssetRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainExplorerRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainExternalApiRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainNodeRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.remote.model.ChainRemote
import io.novafoundation.nova.test_shared.emptyDiff
import io.novafoundation.nova.test_shared.insertsElement
import io.novafoundation.nova.test_shared.removesElement
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ChainSyncServiceTest {

    private val assetId = 0
    private val nodeUrl = "url"
    private val explorerName = "explorer"
    private val transferApiUrl = "url"

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
                staking = listOf("test"),
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
        externalApi = mapOf(
            "history" to listOf(
                ChainExternalApiRemote(
                    sourceType = "subquery",
                    url = transferApiUrl,
                    parameters = null // substrate history
                )
            )
        ),
        explorers = listOf(
            ChainExplorerRemote(
                explorerName,
                "extrinsic",
                "account",
                "event"
            )
        ),
        additional = emptyMap(),
        nodeSelectionStrategy = null
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
            remoteReturns(listOf(REMOTE_CHAIN))

            chainSyncService.syncUp()

            verify(dao).applyDiff(
                chainDiff = insertsChainWithId(REMOTE_CHAIN.chainId),
                assetsDiff = insertsAssetWithId(assetId),
                nodesDiff = insertsNodeWithUrl(nodeUrl),
                explorersDiff = insertsExplorerByName(explorerName),
                externalApisDiff = insertsTransferApiByUrl(transferApiUrl)
            )
        }
    }

    @Test
    fun `should not insert the same chain`() {
        runBlocking {
            localReturns(listOf(LOCAL_CHAIN))
            remoteReturns(listOf(REMOTE_CHAIN))

            chainSyncService.syncUp()

            verify(dao).applyDiff(emptyDiff(), emptyDiff(), emptyDiff(), emptyDiff(), emptyDiff())
        }
    }

    @Test
    fun `should update chain's own params`() {
        runBlocking {
            localReturns(listOf(LOCAL_CHAIN))
            remoteReturns(listOf(REMOTE_CHAIN.copy(name = "new name")))

            chainSyncService.syncUp()

            verify(dao).applyDiff(
                chainDiff = insertsChainWithId(REMOTE_CHAIN.chainId),
                assetsDiff = emptyDiff(),
                nodesDiff = emptyDiff(),
                explorersDiff = emptyDiff(),
                externalApisDiff = emptyDiff()
            )
        }
    }

    @Test
    fun `should update chain's asset`() {
        runBlocking {
            localReturns(listOf(LOCAL_CHAIN))

            remoteReturns(
                listOf(
                    REMOTE_CHAIN.copy(
                        assets = listOf(
                            REMOTE_CHAIN.assets.first().copy(symbol = "NEW")
                        )
                    )
                )
            )

            chainSyncService.syncUp()

            verify(dao).applyDiff(
                chainDiff = emptyDiff(),
                assetsDiff = insertsAssetWithId(assetId),
                nodesDiff = emptyDiff(),
                explorersDiff = emptyDiff(),
                externalApisDiff = emptyDiff()
            )
        }
    }

    @Test
    fun `should update chain's node`() {
        runBlocking {
            localReturns(listOf(LOCAL_CHAIN))

            remoteReturns(
                listOf(
                    REMOTE_CHAIN.copy(
                        nodes = listOf(
                            REMOTE_CHAIN.nodes.first().copy(name = "NEW")
                        )
                    )
                )
            )

            chainSyncService.syncUp()

            verify(dao).applyDiff(
                chainDiff = emptyDiff(),
                assetsDiff = emptyDiff(),
                nodesDiff = insertsNodeWithUrl(nodeUrl),
                explorersDiff = emptyDiff(),
                externalApisDiff = emptyDiff()
            )
        }
    }

    @Test
    fun `should update chain's explorer`() {
        runBlocking {
            localReturns(listOf(LOCAL_CHAIN))

            remoteReturns(
                listOf(
                    REMOTE_CHAIN.copy(
                        explorers = listOf(
                            REMOTE_CHAIN.explorers!!.first().copy(extrinsic = "NEW")
                        )
                    )
                )
            )

            chainSyncService.syncUp()

            verify(dao).applyDiff(
                chainDiff = emptyDiff(),
                assetsDiff = emptyDiff(),
                nodesDiff = emptyDiff(),
                explorersDiff = insertsExplorerByName(explorerName),
                externalApisDiff = emptyDiff()
            )
        }
    }

    @Test
    fun `should update chain's transfer apis`() {
        runBlocking {
            localReturns(listOf(LOCAL_CHAIN))

            val currentHistoryApi = REMOTE_CHAIN.externalApi!!.getValue("history").first()
            val anotherUrl = "another url"

            remoteReturns(
                listOf(
                    REMOTE_CHAIN.copy(
                        externalApi = mapOf(
                            "history" to listOf(
                                currentHistoryApi,
                                currentHistoryApi.copy(url = anotherUrl)
                            )
                        )
                    )
                )
            )

            chainSyncService.syncUp()

            verify(dao).applyDiff(
                chainDiff = emptyDiff(),
                assetsDiff = emptyDiff(),
                nodesDiff = emptyDiff(),
                explorersDiff = emptyDiff(),
                externalApisDiff = insertsTransferApiByUrl(anotherUrl)
            )
        }
    }

    @Test
    fun `should remove chain`() {
        runBlocking {
            localReturns(listOf(LOCAL_CHAIN))

            remoteReturns(emptyList())

            chainSyncService.syncUp()

            verify(dao).applyDiff(
                chainDiff = removesChainWithId(REMOTE_CHAIN.chainId),
                assetsDiff = removesAssetWithId(assetId),
                nodesDiff = removesNodeWithUrl(nodeUrl),
                explorersDiff = removesExplorerByName(explorerName),
                externalApisDiff = removesTransferApiByUrl(transferApiUrl)
            )
        }
    }

    private suspend fun remoteReturns(chains: List<ChainRemote>) {
        `when`(chainFetcher.getChains()).thenReturn(chains)
    }

    private suspend fun localReturns(chains: List<JoinedChainInfo>) {
        `when`(dao.getJoinChainInfo()).thenReturn(chains)
    }

    private fun insertsChainWithId(id: String) = insertsElement<ChainLocal> { it.id == id }
    private fun insertsAssetWithId(id: Int) = insertsElement<ChainAssetLocal> { it.id == id }
    private fun insertsNodeWithUrl(url: String) = insertsElement<ChainNodeLocal> { it.url == url }
    private fun insertsExplorerByName(name: String) = insertsElement<ChainExplorerLocal> { it.name == name }
    private fun insertsTransferApiByUrl(url: String) = insertsElement<ChainExternalApiLocal> { it.url == url }

    private fun removesChainWithId(id: String) = removesElement<ChainLocal> { it.id == id }
    private fun removesAssetWithId(id: Int) = removesElement<ChainAssetLocal> { it.id == id }
    private fun removesNodeWithUrl(url: String) = removesElement<ChainNodeLocal> { it.url == url }
    private fun removesExplorerByName(name: String) = removesElement<ChainExplorerLocal> { it.name == name }
    private fun removesTransferApiByUrl(url: String) = removesElement<ChainExternalApiLocal> { it.url == url }

    private fun createLocalCopy(remote: ChainRemote): JoinedChainInfo {
        val domain = mapRemoteChainToLocal(remote, oldChain = null, source = ChainLocal.Source.DEFAULT, gson)
        val assets = remote.assets.map { mapRemoteAssetToLocal(remote, it, gson, true) }
        val nodes = mapRemoteNodesToLocal(remote)
        val explorers = mapRemoteExplorersToLocal(remote)
        val transferHistoryApis = mapExternalApisToLocal(remote)

        return JoinedChainInfo(
            chain = domain,
            nodes = nodes,
            assets = assets,
            explorers = explorers,
            externalApis = transferHistoryApis
        )
    }
}
