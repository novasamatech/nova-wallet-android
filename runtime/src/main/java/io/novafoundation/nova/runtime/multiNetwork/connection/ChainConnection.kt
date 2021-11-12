package io.novafoundation.nova.runtime.multiNetwork.connection

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.NodeAutobalancer
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import jp.co.soramitsu.fearless_utils.wsrpc.networkStateFlow
import jp.co.soramitsu.fearless_utils.wsrpc.state.SocketStateMachine.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Provider

class ChainConnectionFactory(
    private val externalRequirementFlow: Flow<ChainConnection.ExternalRequirement>,
    private val nodeAutobalancer: NodeAutobalancer,
    private val socketServiceProvider: Provider<SocketService>,
) {

    fun create(chain: Chain): ChainConnection {
        return ChainConnection(
            socketService = socketServiceProvider.get(),
            externalRequirementFlow = externalRequirementFlow,
            nodeAutobalancer = nodeAutobalancer,
            chain = chain
        )
    }
}

class ChainConnection(
    val socketService: SocketService,
    externalRequirementFlow: Flow<ExternalRequirement>,
    nodeAutobalancer: NodeAutobalancer,
    private val chain: Chain,
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    enum class ExternalRequirement {
        ALLOWED, STOPPED, FORBIDDEN
    }

    val state = socketService.networkStateFlow()
        .stateIn(scope = this, started = SharingStarted.Eagerly, initialValue = State.Disconnected)

    private val availableNodes = MutableStateFlow(chain.nodes)
    private val currentNode = nodeAutobalancer.balancingNodeFlow(
        chainId = chain.id,
        socketStateFlow = state,
        availableNodesFlow = availableNodes,
        scope = this
    )

    init {
        externalRequirementFlow.onEach {
            if (it == ExternalRequirement.ALLOWED) {
                socketService.resume()
            } else {
                socketService.pause()
            }
        }
            .launchIn(this)

        setupAutobalancing()
    }

    private fun setupAutobalancing() {
        currentNode
            .distinctUntilChanged()
            .onEach { newNode -> socketService.startOrSwitchTo(newNode) }
            .onEach { Log.d(this@ChainConnection.LOG_TAG, "Switching node in ${chain.name} to ${it.name} (${it.url})") }
            .launchIn(this)
    }

    fun considerUpdateNodes(nodes: List<Chain.Node>) {
        availableNodes.value = nodes
    }

    fun finish() {
        cancel()

        socketService.stop()
    }

    private fun SocketService.startOrSwitchTo(node: Chain.Node) {
        val url = node.url

        if (started()) {
            switchUrl(url)
        } else {
            start(url, remainPaused = true)
        }
    }
}
