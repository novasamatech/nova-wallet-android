package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.ext.isCustomNetwork
import io.novafoundation.nova.runtime.ext.isDisabled
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.ext.selectedUnformattedWssNodeUrlOrNull
import io.novafoundation.nova.runtime.ext.wssNodes
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Nodes.NodeSelectionStrategy
import io.novafoundation.nova.runtime.multiNetwork.connection.node.healthState.NodeHealthStateTesterFactory
import io.novafoundation.nova.runtime.repository.ChainRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext

class ChainNetworkState(
    val chain: Chain,
    val networkCanBeDisabled: Boolean,
    val nodeHealthStates: List<NodeHealthState>,
    val connectingNode: Chain.Node?
)

class NodeHealthState(
    val node: Chain.Node,
    val state: State
) {

    sealed interface State {

        object Connecting : State

        class Connected(val ms: Long) : State

        object Disabled : State
    }
}

interface NetworkManagementChainInteractor {

    fun chainStateFlow(chainId: String, coroutineScope: CoroutineScope): Flow<ChainNetworkState>

    suspend fun toggleAutoBalance(chainId: String)

    suspend fun selectNode(chainId: String, unformattedNodeUrl: String)

    suspend fun toggleChainEnableState(chainId: String)

    suspend fun deleteNetwork(chainId: String)

    suspend fun deleteNode(chainId: String, unformattedNodeUrl: String)
}

class RealNetworkManagementChainInteractor(
    private val chainRegistry: ChainRegistry,
    private val nodeHealthStateTesterFactory: NodeHealthStateTesterFactory,
    private val chainRepository: ChainRepository,
    private val accountInteractor: AccountInteractor
) : NetworkManagementChainInteractor {

    override fun chainStateFlow(chainId: String, coroutineScope: CoroutineScope): Flow<ChainNetworkState> {
        return chainRegistry.chainsById
            .mapNotNull { it[chainId] }
            .flatMapLatest { chain ->
                combine(activeNodeFlow(chainId), nodesHealthState(chain, coroutineScope)) { activeNode, nodeHealthStates ->
                    ChainNetworkState(chain, networkCanBeDisabled(chain), nodeHealthStates, activeNode)
                }
            }
    }

    override suspend fun toggleAutoBalance(chainId: String) {
        val chain = chainRegistry.getChain(chainId)
        chainRegistry.setWssNodeSelectionStrategy(chainId, chain.nodes.strategyForToggledWssAutoBalance())
    }

    private fun Chain.Nodes.strategyForToggledWssAutoBalance(): NodeSelectionStrategy {
        return when (wssNodeSelectionStrategy) {
            NodeSelectionStrategy.AutoBalance -> {
                val firstNode = wssNodes().first()
                NodeSelectionStrategy.SelectedNode(firstNode.unformattedUrl)
            }

            is NodeSelectionStrategy.SelectedNode -> NodeSelectionStrategy.AutoBalance
        }
    }

    override suspend fun selectNode(chainId: String, unformattedNodeUrl: String) {
        val strategy = NodeSelectionStrategy.SelectedNode(unformattedNodeUrl)
        chainRegistry.setWssNodeSelectionStrategy(chainId, strategy)
    }

    override suspend fun toggleChainEnableState(chainId: String) {
        val chain = chainRegistry.getChain(chainId)
        val connectionState = if (chain.isEnabled) Chain.ConnectionState.DISABLED else Chain.ConnectionState.FULL_SYNC
        chainRegistry.changeChainConnectionState(chainId, connectionState)
    }

    override suspend fun deleteNetwork(chainId: String) {
        val chain = chainRegistry.getChain(chainId)

        require(chain.isCustomNetwork)

        withContext(Dispatchers.Default) { accountInteractor.deleteProxiedMetaAccountsByChain(chainId) } // Delete proxied meta accounts manually
        chainRepository.deleteNetwork(chainId)
    }

    override suspend fun deleteNode(chainId: String, unformattedNodeUrl: String) {
        val chain = chainRegistry.getChain(chainId)

        require(chain.nodes.nodes.size > 1)

        chainRepository.deleteNode(chainId, unformattedNodeUrl)

        if (chain.selectedUnformattedWssNodeUrlOrNull == unformattedNodeUrl) {
            chainRegistry.setWssNodeSelectionStrategy(chainId, NodeSelectionStrategy.AutoBalance)
        }
    }

    private fun networkCanBeDisabled(chain: Chain): Boolean {
        return chain.genesisHash != Chain.Geneses.POLKADOT
    }

    private fun nodesHealthState(chain: Chain, coroutineScope: CoroutineScope): Flow<List<NodeHealthState>> {
        return chain.nodes.wssNodes().map {
            nodeHealthState(chain, it, coroutineScope)
        }.combine()
    }

    private fun activeNodeFlow(chainId: String): Flow<Chain.Node?> {
        val activeConnection = chainRegistry.getConnectionOrNull(chainId)
        return activeConnection?.currentUrl?.map { it?.node } ?: flowOf { null }
    }

    private fun nodeHealthState(chain: Chain, node: Chain.Node, coroutineScope: CoroutineScope): Flow<NodeHealthState> {
        return flow {
            if (chain.isDisabled) {
                emit(NodeHealthState(node, NodeHealthState.State.Disabled))
                return@flow
            }

            emit(NodeHealthState(node, NodeHealthState.State.Connecting))

            val nodeConnectionDelay = nodeHealthStateTesterFactory.create(chain, node, coroutineScope)
                .testNodeHealthState()
                .getOrNull()

            nodeConnectionDelay?.let {
                emit(NodeHealthState(node, NodeHealthState.State.Connected(it)))
            }
        }
    }
}
