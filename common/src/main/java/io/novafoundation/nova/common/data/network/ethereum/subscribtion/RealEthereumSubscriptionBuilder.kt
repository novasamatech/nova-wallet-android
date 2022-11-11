package io.novafoundation.nova.common.data.network.ethereum.subscribtion

import io.novafoundation.nova.common.data.network.runtime.binding.cast
import io.novafoundation.nova.common.utils.mergeIfMultiple
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import jp.co.soramitsu.fearless_utils.extensions.asEthereumAddress
import jp.co.soramitsu.fearless_utils.extensions.toAccountId
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService.ResponseListener
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.web3j.protocol.websocket.events.LogNotification
import java.util.UUID

typealias SubscriptionId = String

sealed class EthereumSubscription<S>(val id: SubscriptionId) {

    class Log(val addresses: List<String>, val topics: List<Topic>, id: SubscriptionId) : EthereumSubscription<LogNotification>(id)
}

class EthereumSubscriptionMultiplexer private constructor(
    private val subscriptions: List<EthereumSubscription<*>>,
    private val collectors: Map<SubscriptionId, List<ResponseListener<*>>>
) {

    fun subscribeUsing(web3Api: Web3Api): Flow<*> {
        return subscriptions.map {
            when (it) {
                is EthereumSubscription.Log -> web3Api.subscribeLogs(it)
            }
        }.mergeIfMultiple()
    }

    private fun Web3Api.subscribeLogs(subscription: EthereumSubscription.Log): Flow<*> {
        return logsNotifications(subscription.addresses, subscription.topics).onEach { logNotification ->
            subscription.dispatchChange(logNotification)
        }.catch {
            subscription.dispatchError(it)
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

        fun subscribeLogs(address: String, topics: List<Topic>): Flow<LogNotification> {
            val subscriptions = ensureSubscriptions()
            val existingSubscription = subscriptions.firstOrNull { it is EthereumSubscriptionBuilder.Log && it.topics == topics }

            val subscription = if (existingSubscription != null) {
                require(existingSubscription is EthereumSubscriptionBuilder.Log)
                existingSubscription.addresses.add(address)
                existingSubscription
            } else {
                val newSubscription = EthereumSubscriptionBuilder.Log(topics = topics)
                subscriptions.add(newSubscription)
                newSubscription
            }

            val collector = LogsCallback(address)
            val subscriptionCollectors = ensureCollectors().getOrPut(subscription.id, ::mutableListOf)
            subscriptionCollectors.add(collector)

            return collector.inner.map { it.getOrThrow() }
        }

        fun build() : EthereumSubscriptionMultiplexer {
            return EthereumSubscriptionMultiplexer(
                subscriptions = subscriptions.orEmpty().map { it.build() },
                collectors = collectors.orEmpty()
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

private class LogsCallback(contractAddress: String): FlowCallback<LogNotification>() {

    val contractAccountId = contractAddress.asEthereumAddress().toAccountId().value

    override fun shouldHandle(change: LogNotification): Boolean {
        val changeAddress = change.params.result.address
        val changeAccountId = changeAddress.asEthereumAddress().toAccountId().value

        return contractAccountId.contentEquals(changeAccountId)
    }
}

private abstract class FlowCallback<R> : ResponseListener<R> {

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
