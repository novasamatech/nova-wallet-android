package io.novafoundation.nova.runtime.multiNetwork.connection

import android.util.Log
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.NodeAutobalancer
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import jp.co.soramitsu.fearless_utils.wsrpc.interceptor.WebSocketResponseInterceptor
import jp.co.soramitsu.fearless_utils.wsrpc.interceptor.WebSocketResponseInterceptor.ResponseDelivery
import jp.co.soramitsu.fearless_utils.wsrpc.networkStateFlow
import jp.co.soramitsu.fearless_utils.wsrpc.response.RpcResponse
import jp.co.soramitsu.fearless_utils.wsrpc.state.SocketStateMachine.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Provider

class ChainConnectionFactory(
    private val externalRequirementFlow: Flow<ChainConnection.ExternalRequirement>,
    private val nodeAutobalancer: NodeAutobalancer,
    private val socketServiceProvider: Provider<SocketService>,
    private val connectionSecrets: ConnectionSecrets,
) {

    suspend fun create(chain: Chain): ChainConnection {
        val connection = ChainConnection(
            socketService = socketServiceProvider.get(),
            externalRequirementFlow = externalRequirementFlow,
            nodeAutobalancer = nodeAutobalancer,
            connectionSecrets = connectionSecrets,
            chain = chain
        )

        connection.setup()

        return connection
    }
}

private const val INFURA_ERROR_CODE = -32005
private const val ALCHEMY_ERROR_CODE = 429

private const val BLUST_CAPACITY_ERROR_CODE = -32098
private const val BLUST_RATE_LIMIT_ERROR_CODE = -32097

private val RATE_LIMIT_ERROR_CODES = listOf(
    INFURA_ERROR_CODE,
    ALCHEMY_ERROR_CODE,
    BLUST_CAPACITY_ERROR_CODE,
    BLUST_RATE_LIMIT_ERROR_CODE
)

class ChainConnection internal constructor(
    val socketService: SocketService,
    private val externalRequirementFlow: Flow<ExternalRequirement>,
    nodeAutobalancer: NodeAutobalancer,
    private val chain: Chain,
    private val connectionSecrets: ConnectionSecrets,
) : CoroutineScope by CoroutineScope(Dispatchers.Default),
    WebSocketResponseInterceptor {

    enum class ExternalRequirement {
        ALLOWED, STOPPED
    }

    val state = socketService.networkStateFlow()
        .stateIn(scope = this, started = SharingStarted.Eagerly, initialValue = State.Disconnected)

    private val responseRequiresNodeChangeFlow = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

    private val nodeChangeSignal = merge(
        state.nodeChangeEvents(),
        responseRequiresNodeChangeFlow
    ).shareIn(scope = this, started = SharingStarted.Eagerly)

    private val availableNodes = MutableStateFlow(chain.nodes)

    private val currentUrl = nodeAutobalancer.connectionUrlFlow(
        chainId = chain.id,
        changeConnectionEventFlow = nodeChangeSignal,
        availableNodesFlow = availableNodes,
    )
        .shareIn(scope = this, started = SharingStarted.Eagerly, replay = 1)

    suspend fun setup() {
        socketService.setInterceptor(this)

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
        val firstNodeUrl = currentUrl.first()?.saturatedUrl ?: return
        socketService.start(firstNodeUrl, remainPaused = true)

        currentUrl
            .mapNotNull { it?.saturatedUrl }
            .filter { nodeUrl -> actualUrl() != nodeUrl }
            .onEach { nodeUrl -> socketService.switchUrl(nodeUrl) }
            .onEach { nodeUrl -> Log.d(this@ChainConnection.LOG_TAG, "Switching node in ${chain.name} to $nodeUrl") }
            .launchIn(this)
    }

    fun considerUpdateNodes(nodes: Chain.Nodes) {
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

    private fun Flow<State>.nodeChangeEvents(): Flow<Unit> {
        return mapNotNull { stateValue ->
            Unit.takeIf { stateValue.needsAutobalance() }
        }
    }

    private fun State.needsAutobalance() = this is State.WaitingForReconnect && attempt > 1
    override fun onRpcResponseReceived(rpcResponse: RpcResponse): ResponseDelivery {
        val error = rpcResponse.error

        return if (error != null && error.code in RATE_LIMIT_ERROR_CODES) {
            Log.d(LOG_TAG, "Received rate limit exceeded error code in rpc response. Switching to another node")

            responseRequiresNodeChangeFlow.tryEmit(Unit)

            ResponseDelivery.DROP
        } else {
            ResponseDelivery.DELIVER_TO_SENDER
        }
    }
}
