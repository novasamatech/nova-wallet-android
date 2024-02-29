package io.novafoundation.nova.common.data.network.rpc

import com.google.gson.Gson
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketException
import com.neovisionaries.ws.client.WebSocketFactory
import io.novafoundation.nova.common.base.errors.NovaException
import io.novafoundation.nova.common.resources.ResourceManager
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novasama.substrate_sdk_android.wsrpc.mappers.ResponseMapper
import io.novasama.substrate_sdk_android.wsrpc.request.base.RpcRequest
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Suppress("EXPERIMENTAL_API_USAGE")
class SocketSingleRequestExecutor(
    private val jsonMapper: Gson,
    private val logger: Logger,
    private val wsFactory: WebSocketFactory,
    private val resourceManager: ResourceManager
) {

    suspend fun <R> executeRequest(
        request: RpcRequest,
        url: String,
        mapper: ResponseMapper<R>
    ): R {
        val response = executeRequest(request, url)

        return withContext(Dispatchers.Default) {
            mapper.map(response, jsonMapper)
        }
    }

    suspend fun executeRequest(
        request: RpcRequest,
        url: String
    ): RpcResponse = withContext(Dispatchers.IO) {
        try {
            executeRequestInternal(request, url)
        } catch (e: Exception) {
            throw NovaException.networkError(resourceManager, e)
        }
    }

    private suspend fun executeRequestInternal(
        request: RpcRequest,
        url: String
    ): RpcResponse = suspendCancellableCoroutine { cont ->

        val webSocket: WebSocket = wsFactory.createSocket(url)

        cont.invokeOnCancellation {
            webSocket.clearListeners()
            webSocket.disconnect()
        }

        webSocket.addListener(object : WebSocketAdapter() {
            override fun onTextMessage(websocket: WebSocket, text: String) {
                logger.log("[RECEIVED] $text")

                val response = jsonMapper.fromJson(text, RpcResponse::class.java)

                cont.resume(response)

                webSocket.disconnect()
            }

            override fun onError(websocket: WebSocket, cause: WebSocketException) {
                cont.resumeWithException(cause)
            }
        })

        webSocket.connect()

        webSocket.sendText(jsonMapper.toJson(request))
    }
}
