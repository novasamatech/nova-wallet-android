package io.novafoundation.nova.runtime.multiNetwork.connection.node

import io.novafoundation.nova.common.data.network.rpc.BulkRetriever
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.connection.ConnectionSecrets
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProvider
import io.novafoundation.nova.runtime.multiNetwork.runtime.RuntimeProviderPool
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import javax.inject.Provider

class NodeConnectionFactory(
    private val socketServiceProvider: Provider<SocketService>,
    private val connectionSecrets: ConnectionSecrets,
    private val bulkRetriever: BulkRetriever,
    private val runtimeProviderPool: RuntimeProviderPool,
) {

    fun create(chain: Chain, node: Chain.Node): NodeConnection {
        return if (chain.isEthereumBased) {
            EthereumNodeConnection(
                socketService = socketServiceProvider.get(),
                connectionSecrets = connectionSecrets,
                chain = chain,
                node = node
            )
        } else {
            SubstrateNodeConnection(
                socketService = socketServiceProvider.get(),
                connectionSecrets = connectionSecrets,
                chain = chain,
                bulkRetriever = bulkRetriever,
                runtimeProvider = runtimeProviderPool.getRuntimeProvider(chain.id),
                node = node
            )
        }
    }
}
