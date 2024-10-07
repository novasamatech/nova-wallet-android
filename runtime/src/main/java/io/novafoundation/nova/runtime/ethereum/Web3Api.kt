package io.novafoundation.nova.runtime.ethereum

import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.core.ethereum.log.Topic
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.UpdatableNodes
import io.novafoundation.nova.runtime.multiNetwork.connection.autobalance.strategy.NodeSelectionStrategyProvider
import io.novasama.substrate_sdk_android.extensions.requireHexPrefix
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import okhttp3.OkHttpClient
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
    private val httpClient: OkHttpClient,
    private val strategyProvider: NodeSelectionStrategyProvider,
) {

    fun createWss(socketService: SocketService): Web3Api {
        val web3jService = WebSocketWeb3jService(socketService)

        return RealWeb3Api(
            web3jService = web3jService,
            delegate = Web3j.build(web3jService, JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME.toLong(), requestExecutorService)
        )
    }

    fun createHttps(chainNode: Chain.Node): Pair<Web3Api, UpdatableNodes> {
        val nodes = Chain.Nodes(
            autoBalanceStrategy = Chain.Nodes.AutoBalanceStrategy.ROUND_ROBIN,
            wssNodeSelectionStrategy = Chain.Nodes.NodeSelectionStrategy.AutoBalance,
            nodes = listOf(chainNode)
        )

        return createHttps(nodes)
    }

    fun createHttps(chainNodes: Chain.Nodes): Pair<Web3Api, UpdatableNodes> {
        val service = BalancingHttpWeb3jService(
            initialNodes = chainNodes,
            httpClient = httpClient,
            strategyProvider = strategyProvider,
        )

        val api = RealWeb3Api(
            web3jService = service,
            delegate = Web3j.build(service, JsonRpc2_0Web3j.DEFAULT_BLOCK_TIME.toLong(), requestExecutorService)
        )

        return api to service
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
