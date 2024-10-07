package io.novafoundation.nova.feature_settings_impl.domain.validation

import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.NetworkType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NodeChainIdSingletonHelper(
    private val nodeConnectionSingletonHelper: NodeConnectionSingletonHelper,
    private val nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory
) {

    private var chainId: String? = null
    private val mutex = Mutex()

    suspend fun getChainId(networkType: NetworkType, nodeUrl: String): String {
        return mutex.withLock {
            if (chainId == null) {
                val nodeConnection = nodeConnectionSingletonHelper.getNodeConnection(nodeUrl)
                val nodeChainIdRepository = nodeChainIdRepositoryFactory.create(networkType, nodeConnection)
                chainId = nodeChainIdRepository.requestChainId()
                chainId!!
            } else {
                chainId!!
            }
        }
    }

    fun getChainId(): String {
        return chainId!!
    }
}
