package io.novafoundation.nova.runtime.multiNetwork.connection

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.util.concurrent.ConcurrentHashMap

class ConnectionPool(private val chainConnectionFactory: ChainConnectionFactory) {

    private val pool = ConcurrentHashMap<String, ChainConnection>()

    fun getConnection(chainId: String): ChainConnection = pool.getValue(chainId)

    suspend fun setupConnection(chain: Chain): ChainConnection {
        val connection = pool.getOrPut(chain.id) {
            chainConnectionFactory.create(chain)
        }

        connection.considerUpdateNodes(chain.nodes)

        return connection
    }

    fun removeConnection(chainId: String) {
        pool.remove(chainId)?.apply { finish() }
    }
}
