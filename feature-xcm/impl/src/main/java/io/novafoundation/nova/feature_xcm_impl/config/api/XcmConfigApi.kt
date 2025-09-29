package io.novafoundation.nova.feature_xcm_impl.config.api

import io.novafoundation.nova.feature_xcm_impl.BuildConfig
import retrofit2.http.GET

interface XcmConfigApi {

    @GET(BuildConfig.XCM_GENERAL_CONFIG_URL)
    suspend fun getGeneralXcmConfig(): String
}
