package io.novafoundation.nova.feature_ahm_impl.domain

import io.novafoundation.nova.common.data.repository.ToggleFeatureRepository
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.domain.ChainMigrationConfigUseCase
import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfigWithChains
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class RealChainMigrationConfigUseCase(
    private val migrationInfoRepository: MigrationInfoRepository,
    private val toggleFeatureRepository: ToggleFeatureRepository,
    private val chainRegistry: ChainRegistry
) : ChainMigrationConfigUseCase {

    override fun observeMigrationConfigOrNull(chainId: String, assetId: Int): Flow<ChainMigrationConfigWithChains?> = flowOfAll {
        val config = migrationInfoRepository.getConfigBySource(chainId)
            ?: migrationInfoRepository.getConfigByDestination(chainId)
            ?: return@flowOfAll emptyFlow()

        chainRegistry.observeMigrationConfigWithChains(config)
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
