package io.novafoundation.nova.feature_ahm_api.data.repository

import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfig

interface MigrationInfoRepository {

    suspend fun getConfigBySource(chainId: String): ChainMigrationConfig?

    suspend fun getAllConfigs(): List<ChainMigrationConfig>

    suspend fun loadConfigs()

    suspend fun getConfigByDestination(chainId: String): ChainMigrationConfig?
}
