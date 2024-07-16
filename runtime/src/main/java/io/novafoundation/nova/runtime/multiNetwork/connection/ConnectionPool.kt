package io.novafoundation.nova.runtime.multiNetwork.connection

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.util.concurrent.ConcurrentHashMap

class ConnectionPool(private val chainConnectionFactory: ChainConnectionFactory) {

    private val pool = ConcurrentHashMap<String, ChainConnection>()

    fun getConnection(chainId: String): ChainConnection = pool.getValue(chainId)

    fun getConnectionOrNull(chainId: String): ChainConnection? = pool[chainId]

    suspend fun setupConnection(chain: Chain): ChainConnection {
        val connection = pool.getOrPut(chain.id) {
            chainConnectionFactory.create(chain)
        }

        connection.updateChain(chain)

        return connection
    }

    fun removeConnection(chainId: String) {
        pool.remove(chainId)?.apply { finish() }
    }
}
