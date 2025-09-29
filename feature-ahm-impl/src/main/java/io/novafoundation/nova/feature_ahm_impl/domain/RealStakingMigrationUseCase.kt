package io.novafoundation.nova.feature_ahm_impl.domain

import io.novafoundation.nova.common.data.repository.ToggleFeatureRepository
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.domain.StakingMigrationUseCase
import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfigWithChains
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private const val INFO_STAKING_SHOWN_PREFIX = "migration_staking_info_shown_"

class RealStakingMigrationUseCase(
    private val migrationInfoRepository: MigrationInfoRepository,
    private val toggleFeatureRepository: ToggleFeatureRepository,
    private val chainRegistry: ChainRegistry
) : StakingMigrationUseCase {

    override fun observeMigrationConfigOrNull(chainId: String, assetId: Int): Flow<ChainMigrationConfigWithChains?> = flowOfAll {
        val config = migrationInfoRepository.getConfigByDestination(chainId)
            ?: return@flowOfAll emptyFlow()

        chainRegistry.observeMigrationConfigWithChains(config)
    }

    override fun observeAlertShouldBeHidden(chainId: String, assetId: Int): Flow<Boolean> {
        return toggleFeatureRepository.observe(getKeyFor(chainId, assetId))
    }

    override fun markMigrationInfoAsHidden(chainId: String, assetId: Int) {
        toggleFeatureRepository.set(getKeyFor(chainId, assetId), true)
    }

    private fun getKeyFor(chainId: String, assetId: Int): String {
        return "$INFO_STAKING_SHOWN_PREFIX-$chainId-$assetId"
    }
}
