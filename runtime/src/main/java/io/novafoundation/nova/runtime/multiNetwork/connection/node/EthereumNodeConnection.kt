package io.novafoundation.nova.runtime.multiNetwork.connection.node

import android.util.Log
import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.ethereum.sendSuspend
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateNodeUrl
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime


class EthereumNodeConnection(
    private val chain: Chain,
    private val node: Chain.Node,
    private val connectionSecrets: ConnectionSecrets,
    private val web3ApiFactory: Web3ApiFactory,
    private val socketService: SocketService
) : NodeConnection {

    private val web3Api = createWeb3Api()

    init {
        if (node.connectionType == Chain.Node.ConnectionType.WSS) {
            val saturatedUrlNode = node.saturateNodeUrl(connectionSecrets)

            saturatedUrlNode?.let {
                Log.d("NodeConnection", "Socket started")
                socketService.start(it.saturatedUrl)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun testNodeHealthState(): Result<Long> {
        return runCatching {
            Log.d("NodeConnection", "Testing node connection: ${node.unformattedUrl}")

            val duration = measureTime {
                web3Api.ethBlockNumber().sendSuspend()
            }

            Log.d("NodeConnection", "Node connection tested: ${node.unformattedUrl}")

            duration.inWholeMilliseconds
        }.onFailure {
            Log.e("NodeConnection", "Failed to test node health state", it)
        }
    }

    private fun createWeb3Api(): Web3Api {
        return if (node.connectionType == Chain.Node.ConnectionType.HTTPS) {
            web3ApiFactory.createHttps(node).first
        } else {
            web3ApiFactory.createWss(socketService)
        }
    }
}
