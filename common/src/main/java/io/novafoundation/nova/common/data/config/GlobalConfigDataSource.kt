package io.novafoundation.nova.common.data.config

import io.novafoundation.nova.common.domain.config.GlobalConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface GlobalConfigDataSource {

    suspend fun getGlobalConfig(): GlobalConfig
}

class RealGlobalConfigDataSource(
    private val api: GlobalConfigApi
) : GlobalConfigDataSource {

    private var globalConfig: GlobalConfig? = null
    private val mutex = Mutex()

    override suspend fun getGlobalConfig(): GlobalConfig {
        if (globalConfig != null) return globalConfig!!

        mutex.withLock {
            if (globalConfig != null) return globalConfig!!

            val remoteConfig = api.getGlobalConfig()
            globalConfig = remoteConfig.toDomain()
        }

        return globalConfig!!
    }

    private fun GlobalConfigRemote.toDomain() = GlobalConfig(
        multisigsApiUrl = multisigsApiUrl,
        proxyApiUrl = proxyApiUrl,
        multiStakingApiUrl = multiStakingApiUrl
    )
}
