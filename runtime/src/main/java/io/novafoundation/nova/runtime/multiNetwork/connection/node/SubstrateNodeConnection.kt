package io.novafoundation.nova.runtime.multiNetwork.connection.node

import android.util.Log
import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.common.data.network.rpc.queryKey
import io.novafoundation.nova.common.utils.system
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateNodeUrl
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProvider
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.interceptor.WebSocketResponseInterceptor
import io.novasama.substrate_sdk_android.wsrpc.networkStateFlow
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import io.novasama.substrate_sdk_android.wsrpc.state.SocketStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


class SubstrateNodeConnection(
    private val chain: Chain,
    private val node: Chain.Node,
    private val connectionSecrets: ConnectionSecrets,
    private val bulkRetriever: BulkRetriever,
    private val runtimeProvider: RuntimeProvider,
    val socketService: SocketService
) : NodeConnection, WebSocketResponseInterceptor {

    companion object {
        private val ZERO_BLOCK = 0.toBigInteger()
    }

    init {
        val saturatedUrlNode = node.saturateNodeUrl(connectionSecrets)

        socketService.setInterceptor(this)
        saturatedUrlNode?.let {
            socketService.start(it.saturatedUrl)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun testNodeHealthState(): Result<Long> {
        val runtime = runtimeProvider.get()
        val key = runtime.metadata.system().storage("BlockHash").storageKey(runtime, ZERO_BLOCK)
        return runCatching {
            val duration = measureTime {
                bulkRetriever.queryKey(socketService, key)
            }

            duration.inWholeMilliseconds
        }.onFailure {
            Log.e("NodeConnection", "testNodeHealthState failed", it)
        }
    }

    override fun onRpcResponseReceived(rpcResponse: RpcResponse): WebSocketResponseInterceptor.ResponseDelivery {
        return WebSocketResponseInterceptor.ResponseDelivery.DELIVER_TO_SENDER
    }
}
