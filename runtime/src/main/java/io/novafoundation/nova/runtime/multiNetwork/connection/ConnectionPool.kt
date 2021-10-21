package io.novafoundation.nova.runtime.multiNetwork.connection

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Provider

class ConnectionPool(
    private val socketServiceProvider: Provider<SocketService>,
    private val externalRequirementFlow: MutableStateFlow<ChainConnection.ExternalRequirement>
) {

    private val pool = ConcurrentHashMap<String, ChainConnection>()

    fun getConnection(chainId: String): ChainConnection = pool.getValue(chainId)

    fun setupConnection(chain: Chain): ChainConnection {
        val connection = pool.getOrPut(chain.id) {
            ChainConnection(
                socketService = socketServiceProvider.get(),
                initialNodes = chain.nodes,
                externalRequirementFlow = externalRequirementFlow
            )
        }

        connection.considerUpdateNodes(chain.nodes)

        return connection
    }

    fun removeConnection(chainId: String) {
        pool.remove(chainId)?.apply { finish() }
    }
}
