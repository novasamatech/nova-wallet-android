package io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam

import retrofit2.http.GET

interface MoonbeamApi {

    @GET("https://raw.githubusercontent.com/moonbeam-foundation/crowdloan-self-attestation/main/moonbeam/README.md")
    suspend fun getLegalText(): String
}
