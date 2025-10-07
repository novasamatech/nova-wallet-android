package io.novafoundation.nova.feature_ahm_impl.domain

import io.novafoundation.nova.common.data.repository.ToggleFeatureRepository
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.domain.ChainMigrationInfoUseCase
import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfigWithChains
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.repository.ChainStateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class RealChainMigrationInfoUseCase(
    private val migrationInfoRepository: MigrationInfoRepository,
    private val toggleFeatureRepository: ToggleFeatureRepository,
    private val chainRegistry: ChainRegistry,
    private val chainStateRepository: ChainStateRepository
) : ChainMigrationInfoUseCase {

    override fun observeMigrationConfigOrNull(chainId: String, assetId: Int): Flow<ChainMigrationConfigWithChains?> = flowOfAll {
        val config = migrationInfoRepository.getConfigByOriginChain(chainId)
            ?: migrationInfoRepository.getConfigByDestinationChain(chainId)
            ?: return@flowOfAll emptyFlow()

        if (chainStateRepository.isMigrationBlockNotPassed(config)) return@flowOfAll emptyFlow()

        chainRegistry.chainsById
            .map {
                val sourceChain = it.getValue(config.originData.chainId)
                val destinationChain = it.getValue(config.destinationData.chainId)

                ChainMigrationConfigWithChains(
                    config = config,
                    originChain = sourceChain,
                    originAsset = sourceChain.assetsById.getValue(config.originData.assetId),
                    destinationChain = destinationChain,
                    destinationAsset = destinationChain.assetsById.getValue(config.destinationData.assetId)
                )
            }
    }

    override fun observeInfoShouldBeHidden(key: String, chainId: String, assetId: Int): Flow<Boolean> {
        return toggleFeatureRepository.observe(getKeyFor(key, chainId, assetId))
    }

    override fun markMigrationInfoAsHidden(key: String, chainId: String, assetId: Int) {
        toggleFeatureRepository.set(getKeyFor(key, chainId, assetId), true)
    }

    private fun getKeyFor(key: String, chainId: String, assetId: Int): String {
        return "$key-$chainId-$assetId"
    }
}
