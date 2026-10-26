package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import okhttp3.HttpUrl

internal fun bittensorBackendUrl(base: HttpUrl, path: String): String {
    require(base.isHttps) { "Bittensor infrastructure requires HTTPS" }
    return requireNotNull(base.resolve("/v1/bittensor/$path")).toString()
}

internal suspend fun InfrastructureUrls.bittensorBackendUrl(path: String): String =
    bittensorBackendUrl(baseUrl() ?: throw InfrastructureUrlMissingException(), path)
