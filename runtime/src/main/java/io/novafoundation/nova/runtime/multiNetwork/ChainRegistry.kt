package io.novafoundation.nova.runtime.multiNetwork

import android.util.Log
import com.google.gson.Gson
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.RuntimeContext
import io.novafoundation.nova.common.utils.diffed
import io.novafoundation.nova.common.utils.filterList
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.mapNotNullToSet
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.model.chain.NodeSelectionPreferencesLocal
import io.novafoundation.nova.runtime.ext.isDisabled
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.ext.isFullSync
import io.novafoundation.nova.runtime.ext.level
import io.novafoundation.nova.runtime.ext.requiresBaseTypes
import io.novafoundation.nova.runtime.ext.typesUsage
import io.novafoundation.nova.runtime.multiNetwork.asset.EvmAssetsSyncService
import io.novafoundation.nova.runtime.multiNetwork.chain.ChainSyncService
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapChainLocalToChain
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapConnectionStateToLocal
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.ConnectionState
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Node.ConnectionType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionPool
import io.novafoundation.nova.runtime.multiNetwork.connection.Web3ApiPool
import io.novafoundation.nova.runtime.multiNetwork.exception.DisabledChainException
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProvider
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProviderPool
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeSubscriptionPool
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeSyncService
import io.novafoundation.nova.runtime.multiNetwork.runtime.types.BaseTypeSynchronizer
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

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
            diff.removed.forEach { unregisterChain(it) }
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

        syncBaseTypesIfNeeded()
    }

    fun getConnectionOrNull(chainId: String): ChainConnection? {
        return connectionPool.getConnectionOrNull(chainId.removeHexPrefix())
    }

    @Deprecated("Use getActiveConnectionOrNull, since this method may throw an exception if Chain is disabled")
    suspend fun getActiveConnection(chainId: String): ChainConnection {
        requireConnectionStateAtLeast(chainId, ConnectionState.LIGHT_SYNC)

        return connectionPool.getConnection(chainId.removeHexPrefix())
    }

    suspend fun getActiveConnectionOrNull(chainId: String): ChainConnection? {
        return runCatching {
            requireConnectionStateAtLeast(chainId, ConnectionState.LIGHT_SYNC)

            return connectionPool.getConnectionOrNull(chainId.removeHexPrefix())
        }.getOrNull()
    }

    suspend fun getEthereumApi(chainId: String, connectionType: ConnectionType): Web3Api? {
        return runCatching {
            requireConnectionStateAtLeast(chainId, ConnectionState.LIGHT_SYNC)

            web3ApiPool.getWeb3Api(chainId, connectionType)
        }.getOrNull()
    }

    suspend fun getRuntimeProvider(chainId: String): RuntimeProvider {
        requireConnectionStateAtLeast(chainId, ConnectionState.FULL_SYNC)

        return runtimeProviderPool.getRuntimeProvider(chainId.removeHexPrefix())
    }

    suspend fun getChain(chainId: String): Chain = chainsById.first().getValue(chainId.removeHexPrefix())

    suspend fun enableFullSync(chainId: ChainId) {
        changeChainConnectionState(chainId, ConnectionState.FULL_SYNC)
    }

    suspend fun changeChainConnectionState(chainId: ChainId, state: ConnectionState) {
        val connectionState = mapConnectionStateToLocal(state)
        chainDao.setConnectionState(chainId, connectionState)
    }

    suspend fun setWssNodeSelectionStrategy(chainId: String, strategy: Chain.Nodes.NodeSelectionStrategy) {
        return when (strategy) {
            Chain.Nodes.NodeSelectionStrategy.AutoBalance -> enableAutoBalance(chainId)
            is Chain.Nodes.NodeSelectionStrategy.SelectedNode -> setSelectedNode(chainId, strategy.unformattedNodeUrl)
        }
    }

    private suspend fun enableAutoBalance(chainId: ChainId) {
        chainDao.setNodePreferences(NodeSelectionPreferencesLocal(chainId, autoBalanceEnabled = false, null))
    }

    private suspend fun setSelectedNode(chainId: ChainId, unformattedNodeUrl: String) {
        val chain = getChain(chainId)

        val chainSupportsNode = chain.nodes.nodes.any { it.unformattedUrl == unformattedNodeUrl }
        require(chainSupportsNode) { "Node with url $unformattedNodeUrl is not found for chain $chainId" }

        chainDao.setNodePreferences(NodeSelectionPreferencesLocal(chainId, false, unformattedNodeUrl))
    }

    private suspend fun requireConnectionStateAtLeast(chainId: ChainId, state: ConnectionState) {
        val chain = getChain(chainId)

        if (chain.isDisabled) throw DisabledChainException()
        if (chain.connectionState.level >= state.level) return

        Log.d("ConnectionState", "Requested state $state for ${chain.name}, current is ${chain.connectionState}. Triggering state change to $state")

        chainDao.setConnectionState(chainId, mapConnectionStateToLocal(state))
        awaitConnectionStateIsAtLeast(chainId, state)
    }

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

    private suspend fun awaitConnectionStateIsAtLeast(chainId: ChainId, state: ConnectionState) {
        chainsById
            .mapNotNull { chainsById -> chainsById[chainId] }
            .first { it.connectionState.level >= state.level }
    }

    private fun unregisterChain(chain: Chain) {
        unregisterSubstrateServices(chain)
        unregisterConnections(chain.id)
    }

    private suspend fun registerChain(chain: Chain) {
        return when (chain.connectionState) {
            ConnectionState.FULL_SYNC -> registerFullSyncChain(chain)
            ConnectionState.LIGHT_SYNC -> registerLightSyncChain(chain)
            ConnectionState.DISABLED -> registerDisabledChain(chain)
        }
    }

    private fun registerDisabledChain(chain: Chain) {
        unregisterSubstrateServices(chain)
        unregisterConnections(chain.id)
    }

    private suspend fun registerLightSyncChain(chain: Chain) {
        registerConnection(chain)

        unregisterSubstrateServices(chain)
    }

    private suspend fun registerFullSyncChain(chain: Chain) {
        val connection = registerConnection(chain)

        if (chain.hasSubstrateRuntime) {
            runtimeProviderPool.setupRuntimeProvider(chain)
            runtimeSyncService.registerChain(chain, connection)
            runtimeSubscriptionPool.setupRuntimeSubscription(chain, connection)
        }
    }

    private suspend fun registerConnection(chain: Chain): ChainConnection {
        val connection = connectionPool.setupConnection(chain)

        if (chain.isEthereumBased) {
            web3ApiPool.setupWssApi(chain.id, connection.socketService)
            web3ApiPool.setupHttpsApi(chain)
        }

        return connection
    }

    private fun syncBaseTypesIfNeeded() = launch {
        val chains = currentChains.first()
        val needToSyncBaseTypes = chains.any { it.typesUsage.requiresBaseTypes && it.connectionState.shouldSyncRuntime() }

        if (needToSyncBaseTypes) {
            baseTypeSynchronizer.sync()
        }
    }

    private fun unregisterSubstrateServices(chain: Chain) {
        if (chain.hasSubstrateRuntime) {
            runtimeProviderPool.removeRuntimeProvider(chain.id)
            runtimeSubscriptionPool.removeSubscription(chain.id)
            runtimeSyncService.unregisterChain(chain.id)
        }
    }

    private fun unregisterConnections(chainId: ChainId) {
        connectionPool.removeConnection(chainId)
        web3ApiPool.removeApis(chainId)
    }

    private fun ConnectionState.shouldSyncRuntime(): Boolean {
        return isFullSync
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

suspend fun ChainRegistry.enabledChainWithAssetOrNull(chainId: String, assetId: Int): ChainWithAsset? {
    val chain = getChainOrNull(chainId).takeIf { it?.isEnabled == true } ?: return null
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

suspend fun ChainRegistry.chainWithAsset(fullChainAssetId: FullChainAssetId): ChainWithAsset {
    return chainWithAsset(fullChainAssetId.chainId, fullChainAssetId.assetId)
}

suspend fun ChainRegistry.asset(chainId: String, assetId: Int): Chain.Asset {
    val chain = chainsById.first().getValue(chainId)

    return chain.assetsById.getValue(assetId)
}

suspend fun ChainRegistry.asset(fullChainAssetId: FullChainAssetId): Chain.Asset {
    return asset(fullChainAssetId.chainId, fullChainAssetId.assetId)
}

fun ChainsById.assets(ids: Collection<FullChainAssetId>): List<Chain.Asset> {
    return ids.map { (chainId, assetId) ->
        getValue(chainId).assetsById.getValue(assetId)
    }
}

suspend inline fun <R> ChainRegistry.withRuntime(chainId: ChainId, action: RuntimeContext.() -> R): R {
    return with(RuntimeContext(getRuntime(chainId))) {
        action()
    }
}

suspend inline fun ChainRegistry.findChain(predicate: (Chain) -> Boolean): Chain? = currentChains.first().firstOrNull(predicate)
suspend inline fun ChainRegistry.findChains(predicate: (Chain) -> Boolean): List<Chain> = currentChains.first().filter(predicate)

suspend inline fun ChainRegistry.findChainIds(predicate: (Chain) -> Boolean): Set<ChainId> = currentChains.first().mapNotNullToSet { chain ->
    chain.id.takeIf { predicate(chain) }
}

suspend inline fun ChainRegistry.findChainsById(predicate: (Chain) -> Boolean): ChainsById {
    return chainsById().filterValues { chain -> predicate(chain) }.asChainsById()
}

suspend fun ChainRegistry.getRuntime(chainId: String) = getRuntimeProvider(chainId).get()

suspend fun ChainRegistry.getRawMetadata(chainId: String) = getRuntimeProvider(chainId).getRaw()

suspend fun ChainRegistry.getSocket(chainId: String): SocketService = getActiveConnection(chainId).socketService

suspend fun ChainRegistry.getSocketOrNull(chainId: String): SocketService? = getActiveConnectionOrNull(chainId)?.socketService

suspend fun ChainRegistry.getEthereumApiOrThrow(chainId: String, connectionType: ConnectionType): Web3Api {
    return requireNotNull(getEthereumApi(chainId, connectionType)) {
        "Ethereum Api is not found for chain $chainId and connection type ${connectionType.name}"
    }
}

suspend fun ChainRegistry.getSubscriptionEthereumApiOrThrow(chainId: String): Web3Api {
    return getEthereumApiOrThrow(chainId, ConnectionType.WSS)
}

suspend fun ChainRegistry.getSubscriptionEthereumApi(chainId: String): Web3Api? {
    return getEthereumApi(chainId, ConnectionType.WSS)
}

suspend fun ChainRegistry.getCallEthereumApiOrThrow(chainId: String): Web3Api {
    return getEthereumApi(chainId, ConnectionType.HTTPS)
        ?: getEthereumApiOrThrow(chainId, ConnectionType.WSS)
}

suspend fun ChainRegistry.getCallEthereumApi(chainId: String): Web3Api? {
    return getEthereumApi(chainId, ConnectionType.HTTPS)
        ?: getEthereumApi(chainId, ConnectionType.WSS)
}

suspend fun ChainRegistry.chainsById(): ChainsById = ChainsById(chainsById.first())

suspend fun ChainRegistry.findEvmChain(evmChainId: Int): Chain? {
    return findChain { it.isEthereumBased && it.addressPrefix == evmChainId }
}

suspend fun ChainRegistry.findEvmCallApi(evmChainId: Int): Web3Api? {
    return findEvmChain(evmChainId)?.let {
        getCallEthereumApi(it.id)
    }
}

suspend fun ChainRegistry.findEvmChainFromHexId(evmChainIdHex: String): Chain? {
    val addressPrefix = evmChainIdHex.removeHexPrefix().toIntOrNull(radix = 16) ?: return null

    return findEvmChain(addressPrefix)
}

suspend fun ChainRegistry.findRelayChainOrThrow(chainId: ChainId): ChainId {
    val chain = getChain(chainId)
    return chain.parentId ?: chainId
}

fun ChainRegistry.enabledChainsFlow() = currentChains
    .filterList { it.isEnabled }

suspend fun ChainRegistry.enabledChains() = enabledChainsFlow().first()

fun ChainRegistry.enabledChainByIdFlow() = enabledChainsFlow().map { chains -> chains.associateBy { it.id } }

suspend fun ChainRegistry.enabledChainById() = ChainsById(enabledChainByIdFlow().first())
