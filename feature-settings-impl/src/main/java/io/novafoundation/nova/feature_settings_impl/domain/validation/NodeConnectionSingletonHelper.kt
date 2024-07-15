package io.novafoundation.nova.feature_settings_impl.domain.validation

import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnection
import io.novafoundation.nova.runtime.multiNetwork.connection.node.connection.NodeConnectionFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NodeConnectionSingletonHelper(
    private val nodeConnectionFactory: NodeConnectionFactory,
    private val coroutineScope: CoroutineScope
) {

    private var nodeConnection: NodeConnection? = null
    private val mutex = Mutex()

    suspend fun getNodeConnection(nodeUrl: String): NodeConnection {
        return mutex.withLock {
            if (nodeConnection == null) {
                nodeConnection = nodeConnectionFactory.createNodeConnection(nodeUrl, coroutineScope)
                nodeConnection!!
            } else {
                nodeConnection!!
            }
        }
    }

    fun getNodeConnection(): NodeConnection {
        return nodeConnection!!
    }
}
