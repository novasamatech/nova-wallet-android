package io.novafoundation.nova.core.updater

import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import io.novafoundation.nova.core.model.StorageChange
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import kotlinx.coroutines.flow.Flow
import org.web3j.protocol.websocket.events.LogNotification

interface SubstrateSubscriptionBuilder {

    fun subscribe(key: String): Flow<StorageChange>
}

interface EthereumSubscriptionBuilder {

    val web3Api: Web3Api

    fun subscribeEthLogs(address: String, topics: List<Topic>): Flow<LogNotification>
}

interface SubscriptionBuilder : SubstrateSubscriptionBuilder, EthereumSubscriptionBuilder {

    val socketService: SocketService
}
