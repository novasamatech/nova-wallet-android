package io.novafoundation.nova.app.root.domain

import io.novafoundation.nova.feature_ahm_api.data.repository.ChainMigrationRepository
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository

class MainInteractor(
    private val migrationInfoRepository: MigrationInfoRepository,
    private val chainMigrationRepository: ChainMigrationRepository
) {

    suspend fun getChainIdsToShowMigrationDetails(): List<String> {
        val configs = migrationInfoRepository.getAllConfigs()
        return configs
            .filter { chainMigrationRepository.isNeededToShowInfoForChain(it.sourceData.chainId, it.blockNumberStartAt) }
            .map { it.sourceData.chainId }
    }
}
