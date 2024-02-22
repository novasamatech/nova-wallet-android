package io.novafoundation.nova.runtime.ethereum

import com.fasterxml.jackson.databind.ObjectMapper
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.request.base.RpcRequest
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import org.web3j.protocol.ObjectMapperFactory
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.BatchRequest
import org.web3j.protocol.core.BatchResponse
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.websocket.WebSocketService
import org.web3j.protocol.websocket.events.Notification
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class WebSocketWeb3jService(
    private val socketService: SocketService,
    private val jsonMapper: ObjectMapper = ObjectMapperFactory.getObjectMapper()
) : Web3jService {

    /**
     * Implementation based on [WebSocketService.send]
     */
    override fun <T : Response<*>?> send(request: Request<*, out Response<*>>, responseType: Class<T>): T {
        return try {
            sendAsync(request, responseType).get()
        } catch (e: InterruptedException) {
            Thread.interrupted()
            throw IOException("Interrupted WebSocket request", e)
        } catch (e: ExecutionException) {
            if (e.cause is IOException) {
                throw e.cause as IOException
            }
            throw RuntimeException("Unexpected exception", e.cause)
        }
    }

    override fun <T : Response<*>?> sendAsync(request: Request<*, out Response<*>>, responseType: Class<T>): CompletableFuture<T> {
        val rpcRequest = request.toRpcRequest()

        return socketService.executeRequestAsFuture(rpcRequest).thenApply {
            if (it.error != null) {
                throw EvmRpcException(it.error!!.code, it.error!!.message)
            }

            jsonMapper.convertValue(it, responseType)
        }
    }

    override fun <T : Notification<*>?> subscribe(
        request: Request<*, out Response<*>>,
        unsubscribeMethod: String,
        responseType: Class<T>
    ): Flowable<T> {
        val rpcRequest = request.toRpcRequest()

        return socketService.subscribeAsObservable(rpcRequest, unsubscribeMethod).map {
            jsonMapper.convertValue(it, responseType)
        }.toFlowable(BackpressureStrategy.LATEST)
    }

    override fun sendBatch(batchRequest: BatchRequest): BatchResponse {
        return try {
            sendBatchAsync(batchRequest).get()
        } catch (e: InterruptedException) {
            Thread.interrupted()
            throw IOException("Interrupted WebSocket batch request", e)
        } catch (e: ExecutionException) {
            if (e.cause is IOException) {
                throw e.cause as IOException
            }
            throw RuntimeException("Unexpected exception", e.cause)
        }
    }

    override fun sendBatchAsync(batchRequest: BatchRequest): CompletableFuture<BatchResponse> {
        val rpcRequests = batchRequest.requests.map { it.toRpcRequest() }

        return socketService.executeBatchRequestAsFuture(rpcRequests).thenApply { responses ->
            val responsesById = responses.associateBy(RpcResponse::id)

            val parsedResponses = batchRequest.requests.mapNotNull { request ->
                responsesById[request.id.toInt()]?.let { rpcResponse ->
                    jsonMapper.convertValue(rpcResponse, request.responseType)
                }
            }

            BatchResponse(batchRequest.requests, parsedResponses)
        }
    }

    override fun close() {
        // other components handle lifecycle of socketService
    }

    private fun Request<*, *>.toRpcRequest(): RpcRequest {
        val raw = jsonMapper.writeValueAsString(this)

        return RpcRequest.Raw(raw, id.toInt())
    }
}
