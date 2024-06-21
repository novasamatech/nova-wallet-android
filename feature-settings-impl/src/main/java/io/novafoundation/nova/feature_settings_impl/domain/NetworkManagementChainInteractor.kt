package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.utils.combine
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.runtime.ext.Geneses
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.ext.isDisabled
import io.novafoundation.nova.runtime.ext.isEnabled
import io.novafoundation.nova.runtime.ext.wssNodes
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.node.NodeHealthStateTesterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

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

    fun chainStateFlow(chainId: String): Flow<ChainNetworkState>

    suspend fun toggleAutoBalance(chainId: String)

    suspend fun selectNode(chainId: String, nodeUrl: String)

    suspend fun toggleChainEnableState(chainId: String)

    fun testNode(node: Chain.Node): Chain.Node
}

class RealNetworkManagementChainInteractor(
    private val chainRegistry: ChainRegistry,
    private val nodeHealthStateTesterFactory: NodeHealthStateTesterFactory
) : NetworkManagementChainInteractor {

    override fun chainStateFlow(chainId: String): Flow<ChainNetworkState> {
        return chainRegistry.chainsById
            .map { it.getValue(chainId) }
            .flatMapLatest { chain ->
                combine(activeNodeFlow(chainId), nodesHealthState(chain)) { activeNode, nodeHealthStates ->
                    ChainNetworkState(chain, networkCanBeDisabled(chain), nodeHealthStates, activeNode)
                }
            }
    }

    override suspend fun toggleAutoBalance(chainId: String) {
        val chain = chainRegistry.getChain(chainId)
        val autoBalanceDisabled = !chain.autoBalanceEnabled
        chainRegistry.setAutoBalanceEnabled(chainId, autoBalanceDisabled)

        // If we disable autobalance we need to remove default node
        if (autoBalanceDisabled) {
            chainRegistry.setDefaultNode(chainId, null)
        }
    }

    override suspend fun selectNode(chainId: String, nodeUrl: String) {
        chainRegistry.setDefaultNode(chainId, nodeUrl)
    }

    override suspend fun toggleChainEnableState(chainId: String) {
        val chain = chainRegistry.getChain(chainId)
        val connectionState = if (chain.isEnabled) Chain.ConnectionState.DISABLED else Chain.ConnectionState.FULL_SYNC
        chainRegistry.changeChainConectionState(chainId, connectionState)
    }

    private fun networkCanBeDisabled(chain: Chain): Boolean {
        return chain.genesisHash != Chain.Geneses.POLKADOT
    }

    private fun nodesHealthState(chain: Chain): Flow<List<NodeHealthState>> {
        return chain.nodes.wssNodes().map {
            nodeHealthState(chain, it)
        }.combine()
    }

    private fun activeNodeFlow(chainId: String): Flow<Chain.Node?> {
        val activeConnection = chainRegistry.getConnectionOrNull(chainId)
        return activeConnection?.currentUrl?.map { it?.node } ?: flowOf { null }
    }

    private fun nodeHealthState(chain: Chain, node: Chain.Node): Flow<NodeHealthState> {
        return flow {
            if (chain.isDisabled) {
                emit(NodeHealthState(node, NodeHealthState.State.Disabled))
                return@flow
            }

            emit(NodeHealthState(node, NodeHealthState.State.Connecting))

            val nodeConnectionDelay = nodeHealthStateTesterFactory.create(chain, node)
                .testNodeHealthState()
                .getOrNull()

            nodeConnectionDelay?.let {
                emit(NodeHealthState(node, NodeHealthState.State.Connected(it)))
            }
        }
    }

    override fun testNode(node: Chain.Node): Chain.Node {
        return node
    }
}
