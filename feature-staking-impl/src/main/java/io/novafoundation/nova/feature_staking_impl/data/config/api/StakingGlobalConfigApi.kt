package io.novafoundation.nova.feature_staking_impl.data.config.api

import io.novafoundation.nova.feature_staking_impl.BuildConfig
import io.novafoundation.nova.feature_staking_impl.data.config.api.response.StakingGlobalConfigRemote
import retrofit2.http.GET

interface StakingGlobalConfigApi {

    @GET(BuildConfig.GLOBAL_CONFIG_URL)
    suspend fun getStakingGlobalConfig(): StakingGlobalConfigRemote
}
