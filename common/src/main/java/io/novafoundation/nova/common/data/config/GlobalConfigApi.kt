package io.novafoundation.nova.common.data.config

import io.novafoundation.nova.common.BuildConfig
import retrofit2.http.GET

interface GlobalConfigApi {

    @GET(BuildConfig.GLOBAL_CONFIG_URL)
    suspend fun getGlobalConfig(): GlobalConfigRemote
}
