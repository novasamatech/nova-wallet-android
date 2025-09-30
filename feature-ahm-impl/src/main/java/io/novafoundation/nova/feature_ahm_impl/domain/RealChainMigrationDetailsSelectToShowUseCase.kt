package io.novafoundation.nova.feature_ahm_impl.domain

import io.novafoundation.nova.feature_ahm_api.data.repository.ChainMigrationRepository
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.domain.ChainMigrationDetailsSelectToShowUseCase
import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfig
import io.novafoundation.nova.runtime.repository.ChainStateRepository

class RealChainMigrationDetailsSelectToShowUseCase(
    private val migrationInfoRepository: MigrationInfoRepository,
    private val chainMigrationRepository: ChainMigrationRepository,
    private val chainStateRepository: ChainStateRepository,
) : ChainMigrationDetailsSelectToShowUseCase {

    override suspend fun getChainIdsToShowMigrationDetails(): List<String> {
        val configs = migrationInfoRepository.getAllConfigs()
        return configs
            .filter {
                val detailsWasNotShown = !chainMigrationRepository.isMigrationDetailsWasShown(it.sourceData.chainId)
                val chainRequireMigrationDetails = chainMigrationRepository.isChainMigrationDetailsNeeded(it.sourceData.chainId)
                detailsWasNotShown && chainRequireMigrationDetails && isMigrationBlockPassed(it)
            }
            .map { it.sourceData.chainId }
    }

    private suspend fun isMigrationBlockPassed(config: ChainMigrationConfig): Boolean {
        return chainStateRepository.currentRemoteBlock(config.sourceData.chainId) > config.blockNumberStartAt
    }
}
