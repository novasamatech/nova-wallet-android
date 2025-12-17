package io.novafoundation.nova.feature_ahm_impl.data.config

import io.novafoundation.nova.feature_ahm_impl.BuildConfig
import retrofit2.http.GET

interface ChainMigrationConfigApi {

    @GET(BuildConfig.AHM_CONFIG_URL)
    suspend fun getConfig(): List<ChainMigrationConfigRemote>
}
