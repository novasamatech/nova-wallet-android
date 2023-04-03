package io.novafoundation.nova.core.updater

import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import io.novafoundation.nova.core.model.StorageChange
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import kotlinx.coroutines.flow.Flow
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.websocket.events.LogNotification
import java.util.concurrent.CompletableFuture

interface SubstrateSubscriptionBuilder {

    val socketService: SocketService

    fun subscribe(key: String): Flow<StorageChange>
}

interface EthereumSharedRequestsBuilder {

    val wssEthereumApiIfSupported: Web3Api?

    fun <S, T : Response<*>> ethBatchRequestAsync(batchId: String, request: Request<S, T>): CompletableFuture<T>

    fun subscribeEthLogs(address: String, topics: List<Topic>): Flow<LogNotification>
}

val EthereumSharedRequestsBuilder.wssEthereumApi: Web3Api
    get() = requireNotNull(wssEthereumApiIfSupported) {
        "EthereumSharedRequestsBuilder: wss ethereum api is not supported (chain is not ethereum based)"
    }

interface SharedRequestsBuilder : SubstrateSubscriptionBuilder, EthereumSharedRequestsBuilder
