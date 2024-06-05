package io.novafoundation.nova.runtime.multiNetwork.connection.node

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.connection.saturateNodeUrl
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import kotlin.time.ExperimentalTime


class EthereumNodeConnection(
    private val chain: Chain,
    private val node: Chain.Node,
    private val connectionSecrets: ConnectionSecrets,
    val socketService: SocketService
) : NodeConnection {

    init {
        val saturatedUrlNode = node.saturateNodeUrl(connectionSecrets)

        saturatedUrlNode?.let {
            socketService.switchUrl(it.saturatedUrl)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun testNodeHealthState(): Result<Long> {
        TODO()
    }
}
