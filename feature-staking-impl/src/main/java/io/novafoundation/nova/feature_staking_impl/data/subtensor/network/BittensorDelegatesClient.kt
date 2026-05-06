package io.novafoundation.nova.feature_staking_impl.data.subtensor.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Validator identity record from the `opentensor/bittensor-delegates` GitHub
 * registry. Keys in the parsed map are SS58 addresses of validator hotkeys.
 *
 * Lossy-by-design: the registry is community-curated and most fields are
 * optional. Mirrors `BittensorDelegateMetadata` on iOS.
 */
data class BittensorDelegateMetadata(
    val name: String? = null,
    val url: String? = null,
    val description: String? = null,
    val signature: String? = null,
)

/**
 * Fetches the bittensor-delegates registry. The fetched map is cached so a
 * subsequent network failure can fall back to the last-known good copy.
 *
 * Identity-only — does NOT carry stake / commission / APR. Numeric fields
 * are merged in by `SubtensorValidatorProvider` from a separate data source
 * (TaoStats today, Nova indexer once it ships).
 */
class BittensorDelegatesClient(
    private val httpClient: OkHttpClient,
    private val gson: Gson,
) {

    @Volatile
    private var cache: Map<String, BittensorDelegateMetadata> = emptyMap()

    suspend fun fetchDelegates(): Map<String, BittensorDelegateMetadata> {
        val request = Request.Builder()
            .url(SubtensorStakingConstants.DELEGATES_REGISTRY_URL)
            .get()
            .build()

        // Network failures (DNS / TCP / TLS / timeout) propagate so the caller
        // can log and decide what to surface — silently returning the cached
        // (initially empty) map masks real errors on the very first launch.
        // Non-2xx responses fall back to the cache, mirroring iOS.
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use cache
            val body = response.body?.string() ?: return@use cache
            parse(body).also { cache = it }
        }
    }

    fun cachedDelegates(): Map<String, BittensorDelegateMetadata> = cache

    fun parse(json: String): Map<String, BittensorDelegateMetadata> {
        // JSON parse failures propagate. iOS `BittensorDelegatesClient.swift`
        // throws on parse errors so the caller can log / fall back; Android's
        // call site in `SubtensorStakingViewModel.load` already wraps this in
        // `runCatching` and falls back to an empty identity map. Swallowing
        // here would mask a malformed registry response.
        val type = object : TypeToken<Map<String, BittensorDelegateMetadata>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }
}
