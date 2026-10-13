package io.novafoundation.nova.feature_staking_impl.data.bittensor

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Resolves a Nova Bittensor service route under the attested `/v1/bittensor/` root. */
internal fun bittensorBackendUrl(baseUrl: String, path: String): String {
    val base = requireNotNull(baseUrl.toHttpUrlOrNull()) { "Infrastructure URL is not configured" }
    require(base.isHttps) { "Infrastructure requires HTTPS" }
    return requireNotNull(base.resolve("/v1/bittensor/$path")).toString()
}
