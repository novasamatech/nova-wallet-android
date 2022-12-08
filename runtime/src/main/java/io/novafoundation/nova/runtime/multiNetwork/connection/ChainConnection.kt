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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Provider

class ChainConnectionFactory(
    private val externalRequirementFlow: Flow<ChainConnection.ExternalRequirement>,
    private val nodeAutobalancer: NodeAutobalancer,
    private val socketServiceProvider: Provider<SocketService>,
) {

    suspend fun create(chain: Chain): ChainConnection {
        val connection = ChainConnection(
            socketService = socketServiceProvider.get(),
            externalRequirementFlow = externalRequirementFlow,
            nodeAutobalancer = nodeAutobalancer,
            chain = chain
        )

        connection.setup()

        return connection
    }
}

class ChainConnection internal constructor(
    val socketService: SocketService,
    private val externalRequirementFlow: Flow<ExternalRequirement>,
    nodeAutobalancer: NodeAutobalancer,
    private val chain: Chain,
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    enum class ExternalRequirement {
        ALLOWED, STOPPED
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

    suspend fun setup() {
        observeCurrentNode()

        externalRequirementFlow.onEach {
            if (it == ExternalRequirement.ALLOWED) {
                socketService.resume()
            } else {
                socketService.pause()
            }
        }
            .launchIn(this)
    }

    private suspend fun observeCurrentNode() {
        socketService.start(currentNode.first().url, remainPaused = true)

        currentNode
            .filter { actualUrl() != it.url }
            .onEach { newNode -> socketService.switchUrl(newNode.url) }
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

    private suspend fun actualUrl(): String? {
        return when (val stateSnapshot = state.first()) {
            is State.WaitingForReconnect -> stateSnapshot.url
            is State.Connecting -> stateSnapshot.url
            is State.Connected -> stateSnapshot.url
            State.Disconnected -> null
            is State.Paused -> stateSnapshot.url
        }
    }
}
