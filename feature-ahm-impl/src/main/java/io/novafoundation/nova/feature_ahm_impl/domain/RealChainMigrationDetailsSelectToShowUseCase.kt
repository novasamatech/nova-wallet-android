package io.novafoundation.nova.feature_ahm_impl.domain

import io.novafoundation.nova.feature_ahm_api.data.repository.ChainMigrationRepository
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.domain.ChainMigrationDetailsSelectToShowUseCase
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
                val detailsWasNotShown = !chainMigrationRepository.isMigrationDetailsWasShown(it.originData.chainId)
                val chainRequireMigrationDetails = chainMigrationRepository.isChainMigrationDetailsNeeded(it.originData.chainId)
                detailsWasNotShown && chainRequireMigrationDetails && chainStateRepository.isMigrationBlockPassed(it)
            }
            .map { it.originData.chainId }
    }
}
