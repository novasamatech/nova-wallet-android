package io.novafoundation.nova.runtime.multiNetwork.connection.node

import io.novafoundation.nova.common.utils.emptyEthereumAddress
import io.novafoundation.nova.common.utils.awaitConnected
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.ethereum.sendSuspend
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateNodeUrl
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import org.web3j.protocol.core.DefaultBlockParameterName
import io.novasama.substrate_sdk_android.wsrpc.interceptor.WebSocketResponseInterceptor
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


class EthereumNodeHealthStateTester(
    private val node: Chain.Node,
    private val connectionSecrets: ConnectionSecrets,
    private val web3ApiFactory: Web3ApiFactory,
    private val socketService: SocketService
) : NodeHealthStateTester, WebSocketResponseInterceptor {

    private val web3Api = createWeb3Api()

    init {
        socketService.setInterceptor(this)

        if (node.connectionType == Chain.Node.ConnectionType.WSS) {
            val saturatedUrlNode = node.saturateNodeUrl(connectionSecrets)

            saturatedUrlNode?.let {
                socketService.start(it.saturatedUrl)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun testNodeHealthState(): Result<Long> {
        return runCatching {
            socketService.awaitConnected()

            val duration = measureTime {
                web3Api.ethGetBalance(emptyEthereumAddress(), DefaultBlockParameterName.LATEST).sendSuspend()
            }

            duration.inWholeMilliseconds
        }
    }

    private fun createWeb3Api(): Web3Api {
        return if (node.connectionType == Chain.Node.ConnectionType.HTTPS) {
            web3ApiFactory.createHttps(node).first
        } else {
            web3ApiFactory.createWss(socketService)
        }
    }

    override fun onRpcResponseReceived(rpcResponse: RpcResponse): WebSocketResponseInterceptor.ResponseDelivery {
        return WebSocketResponseInterceptor.ResponseDelivery.DELIVER_TO_SENDER
    }
}
