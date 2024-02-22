package io.novafoundation.nova.runtime.ethereum.subscribtion

import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import io.novasama.substrate_sdk_android.extensions.asEthereumAddress
import io.novasama.substrate_sdk_android.extensions.toAccountId
import io.novasama.substrate_sdk_android.wsrpc.SocketService.ResponseListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.asDeferred
import org.web3j.protocol.core.BatchRequest
import org.web3j.protocol.core.BatchResponse
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.websocket.events.LogNotification
import java.util.UUID
import java.util.concurrent.CompletableFuture

typealias SubscriptionId = String
typealias BatchId = String
typealias RequestId = Int

sealed class EthereumSubscription<S>(val id: SubscriptionId) {

    class Log(val addresses: List<String>, val topics: List<Topic>, id: SubscriptionId) : EthereumSubscription<LogNotification>(id)
}

class EthereumRequestsAggregator private constructor(
    private val subscriptions: List<EthereumSubscription<*>>,
    private val collectors: Map<SubscriptionId, List<ResponseListener<*>>>,
    private val batches: List<PendingBatchRequest>
) {

    fun subscribeUsing(web3Api: Web3Api): Flow<*> {
        return subscriptions.map {
            when (it) {
                is EthereumSubscription.Log -> web3Api.subscribeLogs(it)
            }
        }.mergeIfMultiple()
    }

    fun executeBatches(scope: CoroutineScope, web3Api: Web3Api) {
        batches.forEach { pendingBatchRequest ->
            scope.async {
                val batch = web3Api.newBatch().apply {
                    pendingBatchRequest.requests.forEach {
                        add(it)
                    }
                }

                executeBatch(batch, pendingBatchRequest)
            }
        }
    }

    private fun Web3Api.subscribeLogs(subscription: EthereumSubscription.Log): Flow<*> {
        return logsNotifications(subscription.addresses, subscription.topics).onEach { logNotification ->
            subscription.dispatchChange(logNotification)
        }.catch {
            subscription.dispatchError(it)
        }
    }

    private suspend fun executeBatch(
        batch: BatchRequest,
        pendingBatchRequest: PendingBatchRequest
    ): Result<BatchResponse> {
        return runCatching { batch.sendAsync().asDeferred().await() }
            .onSuccess { batchResponse ->
                batchResponse.responses.onEach { response ->
                    val callback = pendingBatchRequest.callbacks[response.id.toInt()] ?: return@onEach

                    callback.cast<BatchCallback<Any?>>().onNext(response)
                }
            }
            .onFailure { error ->
                pendingBatchRequest.callbacks.values.forEach {
                    it.onError(error)
                }
            }
    }

    private inline fun <reified S> EthereumSubscription<S>.dispatchChange(change: S) {
        val collectors = collectors[id].orEmpty()

        collectors.forEach {
            it.cast<ResponseListener<S>>().onNext(change)
        }
    }

    private fun EthereumSubscription<*>.dispatchError(error: Throwable) {
        val collectors = collectors[id].orEmpty()

        collectors.forEach { it.onError(error) }
    }

    class Builder {

        // We do not initialize them by default to not to allocate arrays and maps when not needed
        private var subscriptions: MutableList<EthereumSubscriptionBuilder<*>>? = null
        private var collectors: MutableMap<SubscriptionId, MutableList<ResponseListener<*>>>? = null

        private var batches: MutableMap<BatchId, PendingBatchRequestBuilder>? = null

        fun subscribeLogs(address: String, topics: List<Topic>): Flow<LogNotification> {
            val subscriptions = ensureSubscriptions()
            val existingSubscription = subscriptions.firstOrNull { it is EthereumSubscriptionBuilder.Log && it.topics == topics }

            val subscription = if (existingSubscription != null) {
                require(existingSubscription is EthereumSubscriptionBuilder.Log)
                existingSubscription.addresses.add(address)
                existingSubscription
            } else {
                val newSubscription = EthereumSubscriptionBuilder.Log(topics = topics, addresses = mutableListOf(address))
                subscriptions.add(newSubscription)
                newSubscription
            }

            val collector = LogsCallback(address)
            val subscriptionCollectors = ensureCollectors().getOrPut(subscription.id, ::mutableListOf)
            subscriptionCollectors.add(collector)

            return collector.inner.map { it.getOrThrow() }
        }

        fun <S, T : Response<*>> batchRequest(batchId: BatchId, request: Request<S, T>): CompletableFuture<T> {
            val batches = ensureBatches()
            val batch = batches.getOrPut(batchId, ::PendingBatchRequestBuilder)

            val callback = BatchCallback<T>()

            batch.requests += request
            batch.callbacks[request.id.toInt()] = callback

            return callback.future
        }

        fun build(): EthereumRequestsAggregator {
            return EthereumRequestsAggregator(
                subscriptions = subscriptions.orEmpty().map { it.build() },
                collectors = collectors.orEmpty(),
                batches = batches.orEmpty().values.map { it.build() }
            )
        }

        private fun ensureSubscriptions(): MutableList<EthereumSubscriptionBuilder<*>> {
            if (subscriptions == null) {
                subscriptions = mutableListOf()
            }

            return subscriptions!!
        }

        private fun ensureCollectors(): MutableMap<SubscriptionId, MutableList<ResponseListener<*>>> {
            if (collectors == null) {
                collectors = mutableMapOf()
            }

            return collectors!!
        }

        private fun ensureBatches(): MutableMap<BatchId, PendingBatchRequestBuilder> {
            if (batches == null) {
                batches = mutableMapOf()
            }

            return batches!!
        }
    }
}

private sealed class EthereumSubscriptionBuilder<S> {

    val id: SubscriptionId = UUID.randomUUID().toString()

    abstract fun build(): EthereumSubscription<S>

    class Log(val topics: List<Topic>, val addresses: MutableList<String> = mutableListOf()) : EthereumSubscriptionBuilder<LogNotification>() {

        override fun build(): EthereumSubscription<LogNotification> {
            return EthereumSubscription.Log(addresses, topics, id)
        }
    }
}

private class LogsCallback(contractAddress: String) : SubscribeCallback<LogNotification>() {

    val contractAccountId = contractAddress.asEthereumAddress().toAccountId().value

    override fun shouldHandle(change: LogNotification): Boolean {
        val changeAddress = change.params.result.address
        val changeAccountId = changeAddress.asEthereumAddress().toAccountId().value

        return contractAccountId.contentEquals(changeAccountId)
    }
}

private class PendingBatchRequest(val requests: List<Request<*, *>>, val callbacks: Map<RequestId, BatchCallback<*>>)

private class PendingBatchRequestBuilder(
    val requests: MutableList<Request<*, *>> = mutableListOf(),
    val callbacks: MutableMap<RequestId, BatchCallback<*>> = mutableMapOf()
) {

    fun build(): PendingBatchRequest = PendingBatchRequest(requests, callbacks)
}

private class BatchCallback<R> : ResponseListener<R> {

    val future = CompletableFuture<R>()

    override fun onError(throwable: Throwable) {
        future.completeExceptionally(throwable)
    }

    override fun onNext(response: R) {
        future.complete(response)
    }
}

private abstract class SubscribeCallback<R> : ResponseListener<R> {

    val inner = MutableSharedFlow<Result<R>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    abstract fun shouldHandle(change: R): Boolean

    override fun onError(throwable: Throwable) {
        inner.tryEmit(Result.failure(throwable))
    }

    override fun onNext(response: R) {
        if (shouldHandle(response)) {
            inner.tryEmit(Result.success(response))
        }
    }
}
