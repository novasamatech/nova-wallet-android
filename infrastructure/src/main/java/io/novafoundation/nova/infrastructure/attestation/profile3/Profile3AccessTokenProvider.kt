package io.novafoundation.nova.infrastructure.attestation.profile3

import okhttp3.HttpUrl

interface Profile3AccessTokenProvider {

    suspend fun token(origin: HttpUrl): Profile3TokenRecord

    /** Invalidates only the sent token in its exact scope; never removes a newer issuance. */
    suspend fun invalidate(record: Profile3TokenRecord)

    /** Records cooldown only. The interceptor owns signal validation and its one request retry. */
    suspend fun recordResponse(record: Profile3TokenRecord, status: Int, code: String?, retryAfter: String?) = Unit

    /** Network-only admission; a fresh HTTP cache hit must remain usable during a request-rate cooldown. */
    suspend fun beforeNetwork(origin: HttpUrl) = Unit
}
