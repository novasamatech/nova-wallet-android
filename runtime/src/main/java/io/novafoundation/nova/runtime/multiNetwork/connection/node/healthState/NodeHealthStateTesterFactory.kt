package io.novafoundation.nova.runtime.multiNetwork.connection.node.healthState

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import javax.inject.Provider
import kotlinx.coroutines.CoroutineScope

class NodeHealthStateTesterFactory(
    private val socketServiceProvider: Provider<SocketService>,
    private val connectionSecrets: ConnectionSecrets,
    private val bulkRetriever: BulkRetriever,
    private val web3ApiFactory: Web3ApiFactory
) {

    fun create(chain: Chain, node: Chain.Node, coroutineScope: CoroutineScope): NodeHealthStateTester {
        val nodeIsSupported = chain.nodes.nodes.any { it.unformattedUrl == node.unformattedUrl }
        require(nodeIsSupported)

        return if (chain.hasSubstrateRuntime) {
            SubstrateNodeHealthStateTester(
                chain = chain,
                socketService = socketServiceProvider.get(),
                connectionSecrets = connectionSecrets,
                bulkRetriever = bulkRetriever,
                node = node,
                coroutineScope = coroutineScope
            )
        } else {
            EthereumNodeHealthStateTester(
                socketService = socketServiceProvider.get(),
                connectionSecrets = connectionSecrets,
                node = node,
                web3ApiFactory = web3ApiFactory,
                coroutineScope = coroutineScope
            )
        }
    }
}
