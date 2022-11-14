package io.novafoundation.nova.core.updater

import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import io.novafoundation.nova.core.model.StorageChange
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.websocket.events.LogNotification

interface SubstrateSubscriptionBuilder {

    fun subscribe(key: String): Flow<StorageChange>
}

interface EthereumSharedRequestsBuilder {

    val web3Api: Web3Api

    fun <S, T: Response<*>> ethBatchRequestAsync(batchId: String, request: Request<S, T>): Deferred<T>

    fun subscribeEthLogs(address: String, topics: List<Topic>): Flow<LogNotification>
}

interface SharedRequestsBuilder : SubstrateSubscriptionBuilder, EthereumSharedRequestsBuilder {

    val socketService: SocketService
}
