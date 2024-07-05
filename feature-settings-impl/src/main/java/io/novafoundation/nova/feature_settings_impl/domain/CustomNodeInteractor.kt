package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import io.novafoundation.nova.feature_settings_impl.domain.validation.NodeChainIdSingletonProvider
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validateNetworkNodeIsAlive
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validateNodeSupportedByNetwork
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNode.NetworkNodeValidationSystem
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNode.validateNetworkNodeIsAlive
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNode.validateNodeNotAdded
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNode.validateNodeSupportedByNetwork
import io.novafoundation.nova.runtime.ext.networkType
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.repository.ChainNodeRepository
import kotlinx.coroutines.CoroutineScope

interface CustomNodeInteractor {

    suspend fun getNodeDetails(chainId: String, nodeUrl: String): Result<Chain.Node>

    suspend fun createNode(chainId: String, url: String, name: String)

    suspend fun updateNode(chainId: String, oldUrl: String, url: String, name: String)

    fun getValidationSystem(coroutineScope: CoroutineScope): NetworkNodeValidationSystem
}

class RealCustomNodeInteractor(
    private val chainRegistry: ChainRegistry,
    private val chainNodeRepository: ChainNodeRepository,
    private val nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory,
) : CustomNodeInteractor {

    override suspend fun getNodeDetails(chainId: String, nodeUrl: String): Result<Chain.Node> {
        return runCatching {
            chainRegistry.getChain(chainId).nodes
                .nodes
                .first { it.unformattedUrl == nodeUrl }
        }
    }

    override suspend fun createNode(chainId: String, url: String, name: String) {
        chainNodeRepository.createChainNode(chainId, url, name)
    }

    override suspend fun updateNode(chainId: String, oldUrl: String, url: String, name: String) {
        chainNodeRepository.saveChainNode(chainId, oldUrl, url, name)
    }

    override fun getValidationSystem(coroutineScope: CoroutineScope): NetworkNodeValidationSystem {
        return ValidationSystem {
            validateNodeNotAdded()

            val chainIdRequestSingleton = NodeChainIdSingletonProvider(nodeChainIdRepositoryFactory, coroutineScope)

            validateNetworkNodeIsAlive { chainIdRequestSingleton.getChainId(it.chain.networkType(), it.nodeUrl) }

            validateNodeSupportedByNetwork { chainIdRequestSingleton.getChainId() }
        }
    }
}
