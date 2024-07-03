package io.novafoundation.nova.feature_settings_impl.domain

import io.novafoundation.nova.common.utils.singletonAction
import io.novafoundation.nova.common.validation.ValidationSystem
import io.novafoundation.nova.feature_settings_impl.data.NodeChainIdRepositoryFactory
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.CustomNetworkValidationSystem
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.NodeChainIdSingletonProvider
import io.novafoundation.nova.feature_settings_impl.domain.validation.customNetwork.validateNetworkNodeIsAlive
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.NetworkType
import io.novafoundation.nova.runtime.repository.ChainNodeRepository
import kotlinx.coroutines.CoroutineScope

interface AddNetworkInteractor {

    suspend fun createSubstrateNetwork(
        nodeUrl: String,
        chainName: String,
        tokenName: String,
        blockExplorer: String?,
        priceInfoLink: String?
    )

    suspend fun createEvmNetwork(
        chainId: String,
        nodeUrl: String,
        chainName: String,
        tokenName: String,
        blockExplorer: String?,
        priceInfoLink: String?
    )

    fun getSubstrateValidationSystem(coroutineScope: CoroutineScope): CustomNetworkValidationSystem

    fun getEvmValidationSystem(coroutineScope: CoroutineScope): CustomNetworkValidationSystem
}

class RealAddNetworkInteractor(
    private val chainRegistry: ChainRegistry,
    private val chainNodeRepository: ChainNodeRepository,
    private val nodeChainIdRepositoryFactory: NodeChainIdRepositoryFactory,
) : AddNetworkInteractor {

    override suspend fun createSubstrateNetwork(nodeUrl: String, chainName: String, tokenName: String, blockExplorer: String?, priceInfoLink: String?) {

    }

    override suspend fun createEvmNetwork(
        chainId: String,
        nodeUrl: String,
        chainName: String,
        tokenName: String,
        blockExplorer: String?,
        priceInfoLink: String?
    ) {

    }

    override fun getSubstrateValidationSystem(coroutineScope: CoroutineScope): CustomNetworkValidationSystem {
        return ValidationSystem {
            // Use singletone here to receive chain id only one time for all vaildations
            val chainIdRequestSingleton = NodeChainIdSingletonProvider(nodeChainIdRepositoryFactory, coroutineScope)

            validateNetworkNodeIsAlive { chainIdRequestSingleton.getChainId(NetworkType.SUBSTRATE, it.nodeUrl) }

            // Validate node is alive

            // Validate chain id isn't added
            // Receive chainId from wss-node and check it's not added

            //Later

            // Validate TOKEN

            // Validate block explorer

            // Validate coingecko link (we has validation for that
        }
    }

    override fun getEvmValidationSystem(coroutineScope: CoroutineScope): CustomNetworkValidationSystem {
        return ValidationSystem {
            // Validate chain id isn't added
            // Easy. New validation

            // Validate network is matched (Wrong Network) and check here that node is Alive
            // Validate node is alive

            val chainIdRequestSingleton = NodeChainIdSingletonProvider(nodeChainIdRepositoryFactory, coroutineScope)

            validateNetworkNodeIsAlive { chainIdRequestSingleton.getChainId(NetworkType.SUBSTRATE, it.nodeUrl) }
            
            // Exist already


            // Later

            // Validate TOKEN
            // Hmm... What to validate here?

            // Validate block explorer

            // Validate coingecko link (we has validation for that
        }
    }
}
