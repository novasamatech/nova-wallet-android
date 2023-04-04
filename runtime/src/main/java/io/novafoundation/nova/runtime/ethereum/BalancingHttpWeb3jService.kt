package io.novafoundation.nova.runtime.ethereum

import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.UpdatableNodes
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.AutoBalanceStrategy
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.AutoBalanceStrategyProvider
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.generateNodeIterator
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateUrl
import io.reactivex.Flowable
import jp.co.soramitsu.fearless_utils.extensions.tryFindNonNull
import okhttp3.Call
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

        return nodeSwitcher.makeRetryingRequest { url ->
            val call = createHttpCall(payload, url)

            call.execute().parseSingleResponse(responseType)
        }
    }

    override fun <T : Response<*>> sendAsync(request: Request<*, out Response<*>>, responseType: Class<T>): CompletableFuture<T> {
        return CompletableFuture.supplyAsync({ send(request, responseType) }, executorService)
    }

    override fun sendBatch(batchRequest: BatchRequest): BatchResponse {
        if (batchRequest.requests.isEmpty()) {
            return BatchResponse(emptyList(), emptyList())
        }

        val payload = objectMapper.writeValueAsString(batchRequest.requests)

        return nodeSwitcher.makeRetryingRequest { url ->
            val call = createHttpCall(payload, url)

            call.execute().parseBatchResponse(batchRequest)
        }
    }

    override fun sendBatchAsync(batchRequest: BatchRequest): CompletableFuture<BatchResponse> {
        return CompletableFuture.supplyAsync({ sendBatch(batchRequest) }, executorService)
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

    private fun createHttpCall(request: String, url: String): Call {
        val mediaType = HttpService.JSON_MEDIA_TYPE
        val requestBody: RequestBody = request.toRequestBody(mediaType)

        val httpRequest: okhttp3.Request = okhttp3.Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        return httpClient.newCall(httpRequest)
    }

    private fun <T : Response<*>> okhttp3.Response.parseSingleResponse(responseType: Class<T>): T {
        val parsedResponse = runCatching {
            body?.let {
                objectMapper.readValue(it.bytes(), responseType)
            }
        }.getOrNull()

        val rpcError = parsedResponse?.error
        if (rpcError != null) {
            throw EvmRpcException(rpcError.code, rpcError.message)
        }

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

        val rpcError = parsedResponses?.tryFindNonNull { it.error }
        if (rpcError != null) {
            throw EvmRpcException(rpcError.code, rpcError.message)
        }

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
    private var nodeIterator = initialStrategy.generateNodeIterator(initialNodes)

    @Volatile
    private var currentNodeUrl = nodeIterator.nextValidNodeUrl()

    @Synchronized
    fun updateNodes(nodes: List<Chain.Node>, strategy: AutoBalanceStrategy) {
        availableNodes = nodes
        balanceStrategy = strategy

        nodeIterator = balanceStrategy.generateNodeIterator(nodes)

        // we do not update currentNode since we want to be lazy here - only update node if it is failing
    }

    @Synchronized
    fun getCurrentNodeUrl(): String {
        return currentNodeUrl
    }

    @Suppress
    fun markCurrentNodeNotAccessible() {
        currentNodeUrl = nodeIterator.nextValidNodeUrl()
    }

    private fun Iterator<Chain.Node>.nextValidNodeUrl(): String {
        while (true) {
            val candidateNode = next()

            val formattedUrl = connectionSecrets.saturateUrl(candidateNode.unformattedUrl)

            if (formattedUrl != null) {
                return formattedUrl
            }
        }
    }
}

private fun <T> NodeSwitcher.makeRetryingRequest(request: (url: String) -> T): T {
    while (true) {
        val url = getCurrentNodeUrl()

        try {
            return request(url)
        } catch (e: Throwable) {
            Log.w("Failed to execute request for url $url", e)

            markCurrentNodeNotAccessible()

            continue
        }
    }
}
