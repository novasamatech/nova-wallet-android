package io.novafoundation.nova.runtime.multiNetwork.connection

import io.novafoundation.nova.core.ethereum.Web3Api
import io.novafoundation.nova.runtime.ethereum.Web3ApiFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Node.ConnectionType
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import java.util.concurrent.ConcurrentHashMap

class Web3ApiPool(private val web3ApiFactory: Web3ApiFactory) {

    private val pool = ConcurrentHashMap<Pair<ChainId, ConnectionType>, Web3Api>()

    fun getWeb3Api(chainId: String, connectionType: ConnectionType): Web3Api? = pool[chainId to connectionType]

    fun setupWssApi(chainId: ChainId, socketService: SocketService): Web3Api {
        return pool.getOrPut(chainId to ConnectionType.WSS) {
            web3ApiFactory.createWss(socketService)
        }
    }

    fun removeApis(chainId: String) {
        ConnectionType.values().forEach {connectionType ->
            pool.remove(chainId to connectionType)
        }
    }
}
