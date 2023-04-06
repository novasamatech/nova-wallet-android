package io.novafoundation.nova.runtime.ethereum

import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import jp.co.soramitsu.fearless_utils.extensions.requireHexPrefix
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import org.web3j.protocol.Web3j
import org.web3j.protocol.Web3jService
import org.web3j.protocol.core.JsonRpc2_0Web3j
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.methods.response.EthSubscribe
import org.web3j.protocol.websocket.events.LogNotification
import org.web3j.protocol.websocket.events.NewHeadsNotification
import org.web3j.utils.Async
import java.util.concurrent.ScheduledExecutorService

class Web3ApiFactory(
    private val requestExecutorService: ScheduledExecutorService = Async.defaultExecutorService(),
) {

    fun createWss(socketService: SocketService): Web3Api {
        val web3jService = WebSocketWeb3jService(socketService)

        return RealWeb3Api(
            web3jService = web3jService,
            delegate = Web3j.build(web3jService, JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME.toLong(), requestExecutorService)
        )
    }
}

internal class RealWeb3Api(
    private val web3jService: Web3jService,
    private val delegate: Web3j
) : Web3Api, Web3j by delegate {
    override fun newHeadsFlow(): Flow<NewHeadsNotification> = newHeadsNotifications().asFlow()

    override fun logsNotifications(addresses: List<String>, topics: List<Topic>): Flow<LogNotification> {
        val logParams = createLogParams(addresses, topics)
        val requestParams = listOf("logs", logParams)

        val request = Request("eth_subscribe", requestParams, web3jService, EthSubscribe::class.java)

        return web3jService.subscribe(request, "eth_unsubscribe", LogNotification::class.java)
            .asFlow()
    }

    private fun createLogParams(addresses: List<String>, topics: List<Topic>): Map<String, Any> {
        return buildMap {
            if (addresses.isNotEmpty()) {
                put("address", addresses)
            }

            if (topics.isNotEmpty()) {
                put("topics", topics.unifyTopics())
            }
        }
    }

    private fun List<Topic>.unifyTopics(): List<Any?> {
        return map { topic ->
            when (topic) {
                Topic.Any -> null
                is Topic.AnyOf -> topic.values.map { it.requireHexPrefix() }
                is Topic.Single -> topic.value.requireHexPrefix()
            }
        }
    }
}
