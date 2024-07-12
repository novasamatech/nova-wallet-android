package io.novafoundation.nova.feature_settings_impl.domain.validation

import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import io.novafoundation.nova.runtime.multiNetwork.chain.model.NetworkType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NodeChainIdSingletonProvider(
    private val nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory,
    private val coroutineScope: CoroutineScope
) {

    private var chainId: String? = null
    private val mutex = Mutex()

    suspend fun getChainId(networkType: NetworkType, nodeUrl: String): String {
        return mutex.withLock {
            if (chainId == null) {
                val nodeChainIdRepository = nodeChainIdRepositoryFactory.create(networkType, nodeUrl, coroutineScope)
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
