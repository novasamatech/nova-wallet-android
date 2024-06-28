package io.novafoundation.nova.runtime.multiNetwork.connection.node

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import javax.inject.Provider

class NodeHealthStateTesterFactory(
    private val socketServiceProvider: Provider<SocketService>,
    private val connectionSecrets: ConnectionSecrets,
    private val bulkRetriever: BulkRetriever,
    private val web3ApiFactory: Web3ApiFactory
) {

    fun create(chain: Chain, node: Chain.Node): NodeHealthStateTester {
        val nodeIsSupported = chain.nodes.nodes.any { it.unformattedUrl == node.unformattedUrl }
        require(nodeIsSupported)

        return if (chain.hasSubstrateRuntime) {
            SubstrateNodeHealthStateTester(
                chain = chain,
                socketService = socketServiceProvider.get(),
                connectionSecrets = connectionSecrets,
                bulkRetriever = bulkRetriever,
                node = node
            )
        } else {
            EthereumNodeHealthStateTester(
                socketService = socketServiceProvider.get(),
                connectionSecrets = connectionSecrets,
                node = node,
                web3ApiFactory = web3ApiFactory
            )
        }
    }
}
