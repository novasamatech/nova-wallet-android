package io.novafoundation.nova.infrastructure.attestation.profile3

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface Profile3Api {

    @POST
    suspend fun challenge(@Url url: String, @Body request: Profile3ChallengeRequest): Profile3ChallengeResponse

    @POST
    suspend fun register(@Url url: String, @Body request: Profile3RegisterRequest): Response<Unit>

    @POST
    suspend fun token(@Url url: String, @Body request: Profile3TokenRequest): Profile3TokenResponse
}
