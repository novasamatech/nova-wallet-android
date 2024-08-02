package io.novafoundation.nova.feature_staking_impl.data.validators

import io.novafoundation.nova.feature_staking_impl.BuildConfig
import retrofit2.http.GET

interface NovaValidatorsApi {

    @GET(BuildConfig.RECOMMENDED_VALIDATORS_URL)
    suspend fun getValidators(): ValidatorsPreferencesRemote
}
