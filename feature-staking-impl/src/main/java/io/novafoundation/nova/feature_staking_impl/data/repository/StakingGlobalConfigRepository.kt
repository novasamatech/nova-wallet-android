package io.novafoundation.nova.feature_staking_impl.data.repository

import io.novafoundation.nova.feature_staking_impl.data.config.StakingGlobalConfig
import io.novafoundation.nova.feature_staking_impl.data.config.api.StakingGlobalConfigApi
import io.novafoundation.nova.feature_staking_impl.data.config.api.response.StakingGlobalConfigRemote
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface StakingGlobalConfigRepository {

    suspend fun getStakingGlobalConfig(): StakingGlobalConfig
}

class RealStakingGlobalConfigRepository(
    private val api: StakingGlobalConfigApi
) : StakingGlobalConfigRepository {

    private val mutex = Mutex()
    private var cache: StakingGlobalConfig? = null

    override suspend fun getStakingGlobalConfig(): StakingGlobalConfig {
        return mutex.withLock {
            if (cache == null) {
                cache = api.getStakingGlobalConfig().toDomain()
            }

            cache!!
        }
    }

    private fun StakingGlobalConfigRemote.toDomain(): StakingGlobalConfig {
        return StakingGlobalConfig(
            multiStakingApiUrl = multiStakingApiUrl
        )
    }
}
