package io.novafoundation.nova.runtime.multiNetwork.connection.node.connection

import io.novafoundation.nova.common.utils.awaitConnected
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateUrl
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.interceptor.WebSocketResponseInterceptor
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import javax.inject.Provider

interface NodeConnection {

    suspend fun awaitConnected()

    fun getSocketService(): SocketService
}

class NodeConnectionFactory(
    private val socketServiceProvider: Provider<SocketService>,
    private val connectionSecrets: ConnectionSecrets
) {

    fun createNodeConnection(nodeUrl: String): NodeConnection {
        return RealNodeConnection(nodeUrl, socketServiceProvider.get(), connectionSecrets)
    }
}

class RealNodeConnection(
    private val nodeUrl: String,
    private val socketService: SocketService,
    private val connectionSecrets: ConnectionSecrets,
) : NodeConnection, WebSocketResponseInterceptor {

    init {
        socketService.setInterceptor(this)

        val saturatedUrlNode = connectionSecrets.saturateUrl(nodeUrl)
        saturatedUrlNode?.let { socketService.start(it) }
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
