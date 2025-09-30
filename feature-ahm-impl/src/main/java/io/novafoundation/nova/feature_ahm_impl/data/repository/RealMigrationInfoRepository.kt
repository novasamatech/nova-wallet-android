package io.novafoundation.nova.feature_ahm_impl.data.repository

import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfig
import io.novafoundation.nova.feature_ahm_impl.data.config.ChainMigrationConfigApi
import io.novafoundation.nova.feature_ahm_impl.data.config.toDomain

class RealMigrationInfoRepository(
    private val api: ChainMigrationConfigApi
) : MigrationInfoRepository {

    private val config = SingleValueCache {
        val configResponse = api.getConfig()
        configResponse.map { it.toDomain() }
    }

    override suspend fun getConfigByOriginChain(chainId: String): ChainMigrationConfig? {
        return getConfigsInternal().getOrNull()?.firstOrNull { it.sourceData.chainId == chainId }
    }

    override suspend fun getConfigByDestinationChain(chainId: String): ChainMigrationConfig? {
        return getConfigsInternal().getOrNull()?.firstOrNull { it.destinationData.chainId == chainId }
    }

    override suspend fun getAllConfigs(): List<ChainMigrationConfig> {
        return getConfigsInternal().getOrNull() ?: emptyList()
    }

    override suspend fun loadConfigs() {
        getConfigsInternal()
    }

    private suspend fun getConfigsInternal() = runCatching {
        config()
    }
}
