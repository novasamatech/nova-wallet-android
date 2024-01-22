package io.novafoundation.nova.runtime.ethereum

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.NodeWithSaturatedUrl
import io.novafoundation.nova.runtime.multiNetwork.connection.UpdatableNodes
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.AutoBalanceStrategy
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.AutoBalanceStrategyProvider
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.generateNodeIterator
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateNodeUrls
import io.reactivex.Flowable
import jp.co.soramitsu.fearless_utils.extensions.tryFindNonNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.web3j.protocol.ObjectMapperFactory
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.BatchRequest
import org.web3j.protocol.core.BatchResponse
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.exceptions.ClientConnectionException
import org.web3j.protocol.http.HttpService
import org.web3j.protocol.websocket.events.Notification
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

class BalancingHttpWeb3jService(
    initialNodes: Chain.Nodes,
    connectionSecrets: ConnectionSecrets,
    private val httpClient: OkHttpClient,
    private val strategyProvider: AutoBalanceStrategyProvider,
    private val objectMapper: ObjectMapper = ObjectMapperFactory.getObjectMapper(),
    private val executorService: ExecutorService,
) : Web3jService, UpdatableNodes {

    private val nodeSwitcher = NodeSwitcher(
        initialNodes = initialNodes.nodes,
        initialStrategy = strategyProvider.strategyFor(initialNodes.nodeSelectionStrategy),
        connectionSecrets = connectionSecrets
    )

    override fun updateNodes(nodes: Chain.Nodes) {
        val autoBalanceStrategy = strategyProvider.strategyFor(nodes.nodeSelectionStrategy)

        nodeSwitcher.updateNodes(nodes.nodes, autoBalanceStrategy)
    }

    override fun <T : Response<*>> send(request: Request<*, out Response<*>>, responseType: Class<T>): T {
        val payload: String = objectMapper.writeValueAsString(request)

        val result = nodeSwitcher.makeRetryingRequest { url ->
            val call = createHttpCall(payload, url)

            call.execute().parseSingleResponse(responseType)
        }

        return result.throwOnRpcError()
    }

    override fun <T : Response<*>> sendAsync(request: Request<*, out Response<*>>, responseType: Class<T>): CompletableFuture<T> {
        val payload: String = objectMapper.writeValueAsString(request)

        return enqueueRetryingRequest(
            payload = payload,
            retriableProcessResponse = { response -> response.parseSingleResponse(responseType) },
            nonRetriableProcessResponse = { it.throwOnRpcError() }
        )
    }

    override fun sendBatch(batchRequest: BatchRequest): BatchResponse {
        if (batchRequest.requests.isEmpty()) {
            return BatchResponse(emptyList(), emptyList())
        }

        val payload = objectMapper.writeValueAsString(batchRequest.requests)

        val result = nodeSwitcher.makeRetryingRequest { url ->
            val call = createHttpCall(payload, url)

            call.execute().parseBatchResponse(batchRequest)
        }

        return result.throwOnRpcError()
    }

    override fun sendBatchAsync(batchRequest: BatchRequest): CompletableFuture<BatchResponse> {
        val payload: String = objectMapper.writeValueAsString(batchRequest.requests)

        return enqueueRetryingRequest(
            payload = payload,
            retriableProcessResponse = { response -> response.parseBatchResponse(batchRequest) },
            nonRetriableProcessResponse = { it.throwOnRpcError() }
        )
    }

    override fun <T : Notification<*>?> subscribe(
        request: Request<*, out Response<*>>,
        unsubscribeMethod: String,
        responseType: Class<T>
    ): Flowable<T> {
        throw UnsupportedOperationException("Http transport does not support subscriptions")
    }

    override fun close() {
        // nothing to close
    }

    private fun <T> enqueueRetryingRequest(
        payload: String,
        retriableProcessResponse: (okhttp3.Response) -> T,
        nonRetriableProcessResponse: (T) -> Unit
    ): CompletableFuture<T> {
        val completableFuture = CallCancellableFuture<T>()

        enqueueRetryingRequest(completableFuture, payload, retriableProcessResponse, nonRetriableProcessResponse)

        return completableFuture
    }

    private fun <T> enqueueRetryingRequest(
        future: CallCancellableFuture<T>,
        payload: String,
        retriableProcessResponse: (okhttp3.Response) -> T,
        nonRetriableProcessResponse: (T) -> Unit
    ) {
        val url = nodeSwitcher.getCurrentNodeUrl() ?: return

        val call = createHttpCall(payload, url)
        future.call = call

        call.enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                if (future.isCancelled) return

                nodeSwitcher.markCurrentNodeNotAccessible()
                enqueueRetryingRequest(future, payload, retriableProcessResponse, nonRetriableProcessResponse)
            }

            override fun onResponse(call: Call, response: okhttp3.Response) {
                if (future.isCancelled) return

                try {
                    val parsedResponse = retriableProcessResponse(response)

                    try {
                        nonRetriableProcessResponse(parsedResponse)

                        future.complete(parsedResponse)
                    } catch (e: Throwable) {
                        future.completeExceptionally(e)
                    }
                } catch (_: Exception) {
                    nodeSwitcher.markCurrentNodeNotAccessible()
                    enqueueRetryingRequest(future, payload, retriableProcessResponse, nonRetriableProcessResponse)
                }
            }
        })
    }

    private fun createHttpCall(request: String, url: String): Call {
        val mediaType = HttpService.JSON_MEDIA_TYPE
        val requestBody: RequestBody = request.toRequestBody(mediaType)

        val httpRequest: okhttp3.Request = okhttp3.Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return httpClient.newCall(httpRequest)
    }

    private fun <T : Response<*>> T.throwOnRpcError(): T {
        val rpcError = error
        if (rpcError != null) {
            throw EvmRpcException(rpcError.code, rpcError.message)
        }

        return this
    }

    private fun BatchResponse.throwOnRpcError(): BatchResponse {
        val rpcError = responses.tryFindNonNull { it.error }
        if (rpcError != null) {
            throw EvmRpcException(rpcError.code, rpcError.message)
        }

        return this
    }

    private fun <T : Response<*>> okhttp3.Response.parseSingleResponse(responseType: Class<T>): T {
        val parsedResponse = runCatching {
            body?.let {
                objectMapper.readValue(it.bytes(), responseType)
            }
        }.getOrNull()

        if (!isSuccessful || parsedResponse == null) {
            throw ClientConnectionException("Invalid response received: $code; ${body?.string()}")
        }

        return parsedResponse
    }

    private fun okhttp3.Response.parseBatchResponse(origin: BatchRequest): BatchResponse {
        val bodyContent = body?.string()

        val parsedResponseResult = runCatching {
            origin.parseResponse(bodyContent!!)
        }

        val parsedResponses = parsedResponseResult.getOrNull()

        if (isSuccessful && parsedResponseResult.isFailure) {
            throw parsedResponseResult.requireException()
        }

        if (!isSuccessful) {
            throw ClientConnectionException("Invalid response received: $code; $bodyContent")
        }

        return BatchResponse(origin.requests, parsedResponses)
    }

    private fun BatchRequest.parseResponse(response: String): List<Response<*>> {
        val requestsById = requests.associateBy { it.id }
        val nodes = objectMapper.readTree(response) as ArrayNode

        return nodes.map { node ->
            val id = (node as ObjectNode).get("id").asLong()
            val request = requestsById.getValue(id)

            objectMapper.treeToValue(node, request.responseType)
        }
    }
}

private class NodeSwitcher(
    initialNodes: List<Chain.Node>,
    initialStrategy: AutoBalanceStrategy,
    private val connectionSecrets: ConnectionSecrets,
) {

    @Volatile
    private var availableNodes: List<Chain.Node> = initialNodes

    @Volatile
    private var balanceStrategy: AutoBalanceStrategy = initialStrategy

    @Volatile
    private var nodeIterator: Iterator<NodeWithSaturatedUrl>? = null

    @Volatile
    private var currentNodeUrl: String? = null

    init {
        updateNodes(initialNodes, initialStrategy)
    }

    @Synchronized
    fun updateNodes(nodes: List<Chain.Node>, strategy: AutoBalanceStrategy) {
        val saturatedNodes = nodes.saturateNodeUrls(connectionSecrets)
        if (saturatedNodes.isEmpty()) return

        availableNodes = nodes
        balanceStrategy = strategy

        nodeIterator = balanceStrategy.generateNodeIterator(saturatedNodes)
        selectNextNode()
    }

    @Synchronized
    fun getCurrentNodeUrl(): String? {
        return currentNodeUrl
    }

    @Suppress
    fun markCurrentNodeNotAccessible() {
        selectNextNode()
    }

    private fun selectNextNode() {
        currentNodeUrl = nodeIterator?.next()?.saturatedUrl
    }
}

private fun <T> NodeSwitcher.makeRetryingRequest(request: (url: String) -> T): T {
    val url = getCurrentNodeUrl() ?: error("No url present to make a request")

    while (true) {
        try {
            return request(url)
        } catch (e: Throwable) {
            Log.w("Failed to execute request for url $url", e)

            markCurrentNodeNotAccessible()

            continue
        }
    }
}

private class CallCancellableFuture<T> : CompletableFuture<T>() {

    var call: Call? = null

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        call?.cancel()

        return super.cancel(mayInterruptIfRunning)
    }
}
