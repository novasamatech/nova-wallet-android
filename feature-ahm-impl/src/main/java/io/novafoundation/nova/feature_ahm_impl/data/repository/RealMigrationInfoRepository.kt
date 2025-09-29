package io.novafoundation.nova.feature_ahm_impl.data.repository

import io.novafoundation.nova.feature_ahm_api.data.repository.MigrationInfoRepository
import io.novafoundation.nova.feature_ahm_api.domain.model.ChainMigrationConfig
import io.novafoundation.nova.feature_ahm_impl.data.config.ChainMigrationConfigApi
import io.novafoundation.nova.feature_ahm_impl.data.config.toDomain
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RealMigrationInfoRepository(
    private val api: ChainMigrationConfigApi
) : MigrationInfoRepository {

    private var configs: List<ChainMigrationConfig>? = null
    private val mutex = Mutex()

    override suspend fun getConfigBySource(chainId: String): ChainMigrationConfig? {
        return getConfigsInternal().firstOrNull { it.sourceData.chainId == chainId }
    }

    override suspend fun getConfigByDestination(chainId: String): ChainMigrationConfig? {
        return getConfigsInternal().firstOrNull { it.destinationData.chainId == chainId }
    }

    override suspend fun getAllConfigs(): List<ChainMigrationConfig> {
        return getConfigsInternal()
    }

    override suspend fun loadConfigs() {
        getConfigsInternal()
    }

    private suspend fun getConfigsInternal(): List<ChainMigrationConfig> {
        if (configs != null) return configs.orEmpty()

        return mutex.withLock {
            if (configs != null) return configs.orEmpty()

            val configResponse = api.getConfig()
            configs = configResponse.map { it.toDomain() }
            configs!!
        }
    }
}
