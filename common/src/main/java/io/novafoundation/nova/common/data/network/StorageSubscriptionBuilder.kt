package io.novafoundation.nova.common.data.network

import io.novafoundation.nova.common.data.network.ethereum.Web3Api
import io.novafoundation.nova.common.data.network.ethereum.subscribtion.EthereumSubscriptionMultiplexer
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invokeOnCompletion
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import io.novafoundation.nova.core.model.StorageChange
import io.novafoundation.nova.core.updater.SubscriptionBuilder
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.storage.StorageSubscriptionMultiplexer
import jp.co.soramitsu.fearless_utils.wsrpc.request.runtime.storage.subscribeUsing
import jp.co.soramitsu.fearless_utils.wsrpc.subscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import org.web3j.protocol.websocket.events.LogNotification
import kotlin.coroutines.CoroutineContext

class StorageSubscriptionBuilder(
    override val socketService: SocketService,
    private val substrateProxy: StorageSubscriptionMultiplexer.Builder,
    private val ethereumProxy: EthereumSubscriptionMultiplexer.Builder,
    override val web3Api: Web3Api = Web3Api(socketService),
) : SubscriptionBuilder {

    companion object {

        fun create(socketService: SocketService): StorageSubscriptionBuilder {
            val substrateProxy = StorageSubscriptionMultiplexer.Builder()
            val ethereumProxy = EthereumSubscriptionMultiplexer.Builder()

            return StorageSubscriptionBuilder(socketService, substrateProxy, ethereumProxy)
        }
    }

    override fun subscribe(key: String): Flow<StorageChange> {
        return substrateProxy.subscribe(key)
            .map { StorageChange(it.block, it.key, it.value) }
    }

    override fun subscribeEthLogs(address: String, topics: List<Topic>): Flow<LogNotification> {
        return ethereumProxy.subscribeLogs(address, topics)
    }

    fun subscribe(coroutineScope: CoroutineScope) {
        ethereumProxy.build().subscribeUsing(web3Api)
            .inBackground()
            .launchIn(coroutineScope)

        val cancellable = socketService.subscribeUsing(substrateProxy.build())
        coroutineScope.invokeOnCompletion { cancellable.cancel() }
    }
}

fun StorageSubscriptionBuilder.subscribe(coroutineContext: CoroutineContext) {
    subscribe(CoroutineScope(coroutineContext))
}
