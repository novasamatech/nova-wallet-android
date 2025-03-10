package io.novafoundation.nova.runtime.multiNetwork.connection

import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.ext.hasHttpNodes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Node.ConnectionType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import java.util.concurrent.ConcurrentHashMap

typealias Web3ApiPoolKey = Pair<ChainId, ConnectionType>
typealias Web3ApiPoolValue = Pair<Web3Api, UpdatableNodes?>

class Web3ApiPool(private val web3ApiFactory: Web3ApiFactory) {

    private val pool = ConcurrentHashMap<Web3ApiPoolKey, Web3ApiPoolValue>()

    fun getWeb3Api(chainId: String, connectionType: ConnectionType): Web3Api? = pool[chainId to connectionType]?.first

    fun setupWssApi(chainId: ChainId, socketService: SocketService): Web3Api {
        return pool.getOrPut(chainId to ConnectionType.WSS) {
            web3ApiFactory.createWss(socketService) to null
        }.first
    }

    fun setupHttpsApi(chain: Chain): Web3Api? {
        val chainNodes = chain.nodes

        if (!chainNodes.hasHttpNodes()) {
            removeApi(chain.id, ConnectionType.HTTPS)

            return null
        }

        val (web3Api, updatableNodes) = pool.getOrPut(chain.id to ConnectionType.HTTPS) {
            web3ApiFactory.createHttps(chainNodes)
        }

        updatableNodes?.updateNodes(chainNodes)

        return web3Api
    }

    fun removeApis(chainId: String) {
        ConnectionType.values().forEach { connectionType ->
            removeApi(chainId, connectionType)
        }
    }

    private fun removeApi(chainId: String, connectionType: ConnectionType) {
        pool.remove(chainId to connectionType)
            .also { it?.first?.shutdown() }
    }
}

interface UpdatableNodes {

    fun updateNodes(nodes: Chain.Nodes)
}
