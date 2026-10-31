package io.novafoundation.nova.feature_staking_impl.data.bittensor

import okhttp3.CacheControl
import retrofit2.Response

/** Missing or non-reusable HTTP policy never grants freshness; Age and transport time consume the TTL. */
internal fun bittensorCacheLifetimeNanos(response: Response<*>): Long {
    val control = CacheControl.parse(response.headers())
    if (control.noStore || control.noCache) return 0L
    val maxAge = control.maxAgeSeconds.coerceIn(0, 300).toLong()
    val ageHeader = response.headers()["Age"]
    val age = if (ageHeader == null) 0L else ageHeader.toLongOrNull()?.coerceAtLeast(0L) ?: return 0L
    return (maxAge - age).coerceAtLeast(0L) * 1_000_000_000L
}
