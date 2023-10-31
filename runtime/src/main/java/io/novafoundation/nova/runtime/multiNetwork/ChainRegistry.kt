package io.novafoundation.nova.runtime.multiNetwork

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.diffed
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.runtime.multiNetwork.asset.EvmAssetsSyncService
import io.novafoundation.nova.runtime.multiNetwork.chain.ChainSyncService
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainLocalToChain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Node.ConnectionType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionPool
import io.novafoundation.nova.runtime.multiNetwork.connection.Web3ApiPool
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProvider
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProviderPool
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeSubscriptionPool
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeSyncService
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.BaseTypeSynchronizer
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

data class ChainService(
    val runtimeProvider: RuntimeProvider,
    val connection: ChainConnection
)

data class ChainWithAsset(
    val chain: Chain,
    val asset: Chain.Asset
)

class ChainRegistry(
    private val runtimeProviderPool: RuntimeProviderPool,
    private val connectionPool: ConnectionPool,
    private val runtimeSubscriptionPool: RuntimeSubscriptionPool,
    private val chainDao: ChainDao,
    private val chainSyncService: ChainSyncService,
    private val evmAssetsSyncService: EvmAssetsSyncService,
    private val baseTypeSynchronizer: BaseTypeSynchronizer,
    private val runtimeSyncService: RuntimeSyncService,
    private val web3ApiPool: Web3ApiPool,
    private val gson: Gson
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    val currentChains = chainDao.joinChainInfoFlow()
        .mapList { mapChainLocalToChain(it, gson) }
        .diffed()
        .map { diff ->
            diff.removed.forEach { unregisterChain(it.id) }
            diff.newOrUpdated.forEach { chain -> registerChain(chain) }

            diff.all
        }
        .filter { it.isNotEmpty() }
        .distinctUntilChanged()
        .inBackground()
        .shareIn(this, SharingStarted.Eagerly, replay = 1)

    val chainsById = currentChains.map { chains -> chains.associateBy { it.id } }
        .inBackground()
        .shareIn(this, SharingStarted.Eagerly, replay = 1)

    init {
        syncChainsAndAssets()

        baseTypeSynchronizer.sync()
    }

    fun getConnection(chainId: String): ChainConnection = connectionPool.getConnection(chainId.removeHexPrefix())

    fun getConnectionOrNull(chainId: String): ChainConnection? = connectionPool.getConnectionOrNull(chainId.removeHexPrefix())

    fun getRuntimeProvider(chainId: String) = runtimeProviderPool.getRuntimeProvider(chainId.removeHexPrefix())

    fun getEthereumApi(chainId: String, connectionType: ConnectionType): Web3Api? {
        return web3ApiPool.getWeb3Api(chainId, connectionType)
    }

    suspend fun getChain(chainId: String): Chain = chainsById.first().getValue(chainId.removeHexPrefix())

    private fun syncChainsAndAssets() {
        launch {
            runCatching {
                chainSyncService.syncUp()
                evmAssetsSyncService.syncUp()
            }.onFailure {
                Log.e(LOG_TAG, "Failed to sync chains or assets", it)
            }
        }
    }

    private fun unregisterChain(chainId: ChainId) {
        runtimeProviderPool.removeRuntimeProvider(chainId)
        runtimeSubscriptionPool.removeSubscription(chainId)
        runtimeSyncService.unregisterChain(chainId)
        connectionPool.removeConnection(chainId)

        web3ApiPool.removeApis(chainId)
    }

    private suspend fun registerChain(chain: Chain) {
        val connection = connectionPool.setupConnection(chain)

        if (chain.hasSubstrateRuntime) {
            runtimeProviderPool.setupRuntimeProvider(chain)
            runtimeSyncService.registerChain(chain, connection)
            runtimeSubscriptionPool.setupRuntimeSubscription(chain, connection)
        }

        if (chain.isEthereumBased) {
            web3ApiPool.setupWssApi(chain.id, connection.socketService)
            web3ApiPool.setupHttpsApi(chain)
        }
    }
}

suspend fun ChainRegistry.getChainOrNull(chainId: String): Chain? {
    return chainsById.first()[chainId.removeHexPrefix()]
}

suspend fun ChainRegistry.chainWithAssetOrNull(chainId: String, assetId: Int): ChainWithAsset? {
    val chain = getChainOrNull(chainId) ?: return null
    val chainAsset = chain.assetsById[assetId] ?: return null

    return ChainWithAsset(chain, chainAsset)
}

suspend fun ChainRegistry.assetOrNull(fullChainAssetId: FullChainAssetId): Chain.Asset? {
    val chain = getChainOrNull(fullChainAssetId.chainId) ?: return null

    return chain.assetsById[fullChainAssetId.assetId]
}

suspend fun ChainRegistry.chainWithAsset(chainId: String, assetId: Int): ChainWithAsset {
    val chain = chainsById.first().getValue(chainId)

    return ChainWithAsset(chain, chain.assetsById.getValue(assetId))
}

suspend fun ChainRegistry.asset(chainId: String, assetId: Int): Chain.Asset {
    val chain = chainsById.first().getValue(chainId)

    return chain.assetsById.getValue(assetId)
}

suspend fun ChainRegistry.asset(fullChainAssetId: FullChainAssetId): Chain.Asset {
    return asset(fullChainAssetId.chainId, fullChainAssetId.assetId)
}

suspend fun ChainRegistry.assets(ids: Collection<FullChainAssetId>): List<Chain.Asset> {
    val chains = chainsById()

    return ids.map { (chainId, assetId) ->
        chains.getValue(chainId).assetsById.getValue(assetId)
    }
}

suspend inline fun ChainRegistry.findChain(predicate: (Chain) -> Boolean): Chain? = currentChains.first().firstOrNull(predicate)
suspend inline fun ChainRegistry.findChains(predicate: (Chain) -> Boolean): List<Chain> = currentChains.first().filter(predicate)

suspend inline fun ChainRegistry.findChainsById(predicate: (Chain) -> Boolean): ChainsById {
    return chainsById().filterValues { chain -> predicate(chain) }.asChainsById()
}

suspend fun ChainRegistry.getRuntime(chainId: String) = getRuntimeProvider(chainId).get()

fun ChainRegistry.getSocket(chainId: String): SocketService = getConnection(chainId).socketService

fun ChainRegistry.getSocketOrNull(chainId: String): SocketService? = getConnectionOrNull(chainId)?.socketService

suspend fun ChainRegistry.awaitChains() {
    chainsById.first()
}

suspend fun ChainRegistry.awaitSocket(chainId: String): SocketService {
    awaitChains()

    return getSocket(chainId)
}

suspend fun ChainRegistry.awaitSocketOrNull(chainId: String): SocketService? {
    awaitChains()

    return getSocketOrNull(chainId)
}

suspend fun ChainRegistry.awaitEthereumApi(chainId: String, connectionType: ConnectionType): Web3Api? {
    awaitChains()

    return getEthereumApi(chainId, connectionType)
}

suspend fun ChainRegistry.awaitEthereumApiOrThrow(chainId: String, connectionType: ConnectionType): Web3Api {
    return requireNotNull(awaitEthereumApi(chainId, connectionType)) {
        "Ethereum Api is not found for chain $chainId and connection type ${connectionType.name}"
    }
}

suspend fun ChainRegistry.awaitSubscriptionEthereumApiOrThrow(chainId: String): Web3Api {
    return awaitEthereumApiOrThrow(chainId, ConnectionType.WSS)
}

suspend fun ChainRegistry.awaitSubscriptionEthereumApi(chainId: String): Web3Api? {
    return awaitEthereumApi(chainId, ConnectionType.WSS)
}

suspend fun ChainRegistry.awaitCallEthereumApiOrThrow(chainId: String): Web3Api {
    return awaitEthereumApi(chainId, ConnectionType.HTTPS)
        ?: awaitEthereumApiOrThrow(chainId, ConnectionType.WSS)
}

suspend fun ChainRegistry.awaitCallEthereumApi(chainId: String): Web3Api? {
    return awaitEthereumApi(chainId, ConnectionType.HTTPS)
        ?: awaitEthereumApi(chainId, ConnectionType.WSS)
}

suspend fun ChainRegistry.chainsById(): ChainsById = ChainsById(chainsById.first())

fun ChainRegistry.getService(chainId: String) = ChainService(
    runtimeProvider = getRuntimeProvider(chainId),
    connection = getConnection(chainId)
)

suspend fun ChainRegistry.findEvmChain(evmChainId: Int): Chain? {
    return findChain { it.isEthereumBased && it.addressPrefix == evmChainId }
}

suspend fun ChainRegistry.findEvmCallApi(evmChainId: Int): Web3Api? {
    return findEvmChain(evmChainId)?.let {
        awaitCallEthereumApi(it.id)
    }
}

suspend fun ChainRegistry.findEvmChainFromHexId(evmChainIdHex: String): Chain? {
    val addressPrefix = evmChainIdHex.removeHexPrefix().toIntOrNull(radix = 16) ?: return null

    return findEvmChain(addressPrefix)
}
