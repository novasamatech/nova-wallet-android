package io.novafoundation.nova.core.updater

import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import io.novafoundation.nova.core.model.StorageChange
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import kotlinx.coroutines.flow.Flow
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.websocket.events.LogNotification
import java.util.concurrent.CompletableFuture

interface SubstrateSubscriptionBuilder {

    val socketService: SocketService?

    fun subscribe(key: String): Flow<StorageChange>
}

interface EthereumSharedRequestsBuilder {

    val callApi: Web3Api?

    val subscriptionApi: Web3Api?

    fun <S, T : Response<*>> ethBatchRequestAsync(batchId: String, request: Request<S, T>): CompletableFuture<T>

    fun subscribeEthLogs(address: String, topics: List<Topic>): Flow<LogNotification>
}

val EthereumSharedRequestsBuilder.callApiOrThrow: Web3Api
    get() = requireNotNull(callApi) {
        "Chain doesn't have any ethereum apis available"
    }

interface SharedRequestsBuilder : SubstrateSubscriptionBuilder, EthereumSharedRequestsBuilder
