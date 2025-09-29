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
        return getConfigsInternal().getOrNull()?.firstOrNull { it.sourceData.chainId == chainId }
    }

    override suspend fun getConfigByDestination(chainId: String): ChainMigrationConfig? {
        return getConfigsInternal().getOrNull()?.firstOrNull { it.destinationData.chainId == chainId }
    }

    override suspend fun getAllConfigs(): List<ChainMigrationConfig> {
        return getConfigsInternal().getOrNull() ?: emptyList()
    }

    override suspend fun loadConfigs() {
        getConfigsInternal()
    }

    private suspend fun getConfigsInternal(): Result<List<ChainMigrationConfig>> = runCatching {
        if (configs != null) return@runCatching configs.orEmpty()

        return@runCatching mutex.withLock {
            if (configs != null) return@runCatching configs.orEmpty()

            val configResponse = api.getConfig()
            configs = configResponse.map { it.toDomain() }
            configs!!
        }
    }
}
