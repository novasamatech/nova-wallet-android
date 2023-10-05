package io.novafoundation.nova.runtime.multiNetwork.asset

import com.google.gson.Gson
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.model.chain.AssetSourceLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.runtime.multiNetwork.asset.remote.AssetFetcher
import io.novafoundation.nova.runtime.multiNetwork.asset.remote.model.EVMAssetRemote
import io.novafoundation.nova.runtime.multiNetwork.asset.remote.model.EVMInstanceRemote
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.chainAssetIdOfErc20Token
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapEVMAssetRemoteToLocalAssets
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.test_shared.emptyDiff
import io.novafoundation.nova.test_shared.insertsElement
import io.novafoundation.nova.test_shared.removesElement
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class EvmErc20AssetSyncServiceTest {

    private val chainId = "chainId"
    private val contractAddress = "0xc748673057861a797275CD8A068AbB95A902e8de"
    private val assetId = chainAssetIdOfErc20Token(contractAddress)
    private val buyProviders = mapOf("transak" to mapOf("network" to "ETHEREUM"))

    private val REMOTE_ASSET = EVMAssetRemote(
        symbol = "USDT",
        precision = 6,
        priceId = "usd",
        name = "USDT",
        icon = "https://url.com",
        instances = listOf(
            EVMInstanceRemote(chainId, contractAddress, buyProviders)
        )
    )

    private val gson = Gson()

    private val LOCAL_ASSETS = createLocalCopy(REMOTE_ASSET)

    @Mock
    lateinit var dao: ChainAssetDao

    @Mock
    lateinit var chaindao: ChainDao

    @Mock
    lateinit var assetFetcher: AssetFetcher

    lateinit var evmAssetSyncService: EvmAssetsSyncService

    @Before
    fun setup() {
        evmAssetSyncService = EvmAssetsSyncService(chaindao, dao, assetFetcher, gson)
    }

    @Test
    fun `should insert new asset`() {
        runBlocking {
            localHasChains(chainId)
            localReturnsERC20(emptyList())
            remoteReturns(listOf(REMOTE_ASSET))

            evmAssetSyncService.syncUp()

            verify(dao).updateAssets(
                insertAsset(chainId, assetId),
            )
        }
    }

    @Test
    fun `should not insert the same asset`() {
        runBlocking {
            localHasChains(chainId)
            localReturnsERC20(LOCAL_ASSETS)
            remoteReturns(listOf(REMOTE_ASSET))

            evmAssetSyncService.syncUp()

            verify(dao).updateAssets(emptyDiff())
        }
    }

    @Test
    fun `should update assets's own params`() {
        runBlocking {
            localHasChains(chainId)
            localReturnsERC20(LOCAL_ASSETS)
            remoteReturns(listOf(REMOTE_ASSET.copy(name = "new name")))

            evmAssetSyncService.syncUp()

            verify(dao).updateAssets(
                insertAsset(chainId, assetId),
            )
        }
    }

    @Test
    fun `should remove asset`() {
        runBlocking {
            localHasChains(chainId)
            localReturnsERC20(LOCAL_ASSETS)
            remoteReturns(emptyList())

            evmAssetSyncService.syncUp()

            verify(dao).updateAssets(
                removeAsset(chainId, assetId),
            )
        }
    }

    @Test
    fun `should not overwrite enabled state`() {
        runBlocking {
            localHasChains(chainId)
            localReturnsERC20(LOCAL_ASSETS.map { it.copy(enabled = false) })
            remoteReturns(listOf(REMOTE_ASSET))

            evmAssetSyncService.syncUp()

            verify(dao).updateAssets(
                emptyDiff(),
            )
        }
    }

    @Test
    fun `should not modify manual assets`() {
        runBlocking {
            localHasChains(chainId)
            localReturnsERC20(LOCAL_ASSETS)
            localReturnsManual(emptyList())
            remoteReturns(listOf(REMOTE_ASSET))

            evmAssetSyncService.syncUp()

            verify(dao).updateAssets(
                emptyDiff(),
            )
        }
    }

    @Test
    fun `should not insert assets for non-present chain`() {
        runBlocking {
            localHasChains(chainId)
            localReturnsERC20(emptyList())
            remoteReturns(listOf(REMOTE_ASSET.copy(instances = listOf(EVMInstanceRemote("changedChainId", contractAddress, buyProviders)))))

            evmAssetSyncService.syncUp()

            verify(dao).updateAssets(emptyDiff())
        }
    }

    private suspend fun remoteReturns(assets: List<EVMAssetRemote>) {
        `when`(assetFetcher.getEVMAssets()).thenReturn(assets)
    }

    private suspend fun localReturnsERC20(assets: List<ChainAssetLocal>) {
        `when`(dao.getAssetsBySource(AssetSourceLocal.ERC20)).thenReturn(assets)
    }

    private suspend fun localHasChains(vararg chainIds: ChainId) {
        `when`(chaindao.getAllChainIds()).thenReturn(chainIds.toList())
    }

    private suspend fun localReturnsManual(assets: List<ChainAssetLocal>) {
        lenient().`when`(dao.getAssetsBySource(AssetSourceLocal.MANUAL)).thenReturn(assets)
    }

    private fun insertAsset(chainId: String, id: Int) = insertsElement<ChainAssetLocal> { it.chainId == chainId && it.id == id }

    private fun removeAsset(chainId: String, id: Int) = removesElement<ChainAssetLocal> { it.chainId == chainId && it.id == id }

    private fun createLocalCopy(remote: EVMAssetRemote): List<ChainAssetLocal> {
        return mapEVMAssetRemoteToLocalAssets(remote, gson)
    }
}
