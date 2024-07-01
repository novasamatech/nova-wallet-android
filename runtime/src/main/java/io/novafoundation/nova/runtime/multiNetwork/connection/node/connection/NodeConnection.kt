package io.novafoundation.nova.runtime.multiNetwork.connection.node.connection

import io.novafoundation.nova.common.utils.awaitConnected
import io.novafoundation.nova.common.utils.invokeOnCompletion
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateUrl
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.interceptor.WebSocketResponseInterceptor
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import javax.inject.Provider
import kotlinx.coroutines.CoroutineScope

interface NodeConnection {

    suspend fun awaitConnected()

    fun getSocketService(): SocketService
}

class NodeConnectionFactory(
    private val socketServiceProvider: Provider<SocketService>,
    private val connectionSecrets: ConnectionSecrets
) {

    fun createNodeConnection(nodeUrl: String, coroutineScope: CoroutineScope): NodeConnection {
        return RealNodeConnection(nodeUrl, socketServiceProvider.get(), connectionSecrets, coroutineScope)
    }
}

class RealNodeConnection(
    private val nodeUrl: String,
    private val socketService: SocketService,
    private val connectionSecrets: ConnectionSecrets,
    private val coroutineScope: CoroutineScope
) : NodeConnection, WebSocketResponseInterceptor {

    init {
        socketService.setInterceptor(this)
        val saturatedUrlNode = connectionSecrets.saturateUrl(nodeUrl)
        saturatedUrlNode?.let {
            socketService.start(it)
            coroutineScope.invokeOnCompletion { socketService.stop() }
        }
    }

    override fun getSocketService(): SocketService {
        return socketService
    }

    override suspend fun awaitConnected() {
        socketService.awaitConnected()
    }

    override fun onRpcResponseReceived(rpcResponse: RpcResponse): WebSocketResponseInterceptor.ResponseDelivery {
        return WebSocketResponseInterceptor.ResponseDelivery.DELIVER_TO_SENDER
    }
}
