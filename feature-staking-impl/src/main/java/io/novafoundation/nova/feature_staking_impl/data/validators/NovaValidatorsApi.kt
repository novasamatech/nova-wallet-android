package io.novafoundation.nova.feature_staking_impl.data.validators

import retrofit2.http.GET

interface NovaValidatorsApi {

    @GET("https://raw.githubusercontent.com/novasamatech/nova-utils/master/staking/nova_validators.json")
    suspend fun getValidators(): Map<String, List<String>>
}
