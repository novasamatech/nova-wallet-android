package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.CacheControl
import okhttp3.HttpUrl
import retrofit2.HttpException
import retrofit2.Response

/** Only HTTP-fresh data is reused; stale data never supplies a fallback eligibility policy. */
class BittensorValidatorBackend(private val api: BittensorValidatorsApi, private val infrastructureUrls: InfrastructureUrls) {

    private class Cached<T>(val data: T, val deadlineNanos: Long)

    private val mutex = Mutex()

    private var cachedBaseUrl: HttpUrl? = null

    private val lists = mutableMapOf<Int, Cached<BittensorValidatorsRemote>>()

    private var recommendations: Cached<BittensorRecommendationsRemote>? = null

    private var validatorRecommendations: Cached<BittensorRecommendationsRemote>? = null

    suspend fun validators(netuid: Int): BittensorValidatorsRemote = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val url = endpoint(baseUrl, "subnets/$netuid/validators")
        val freshEntry = lists[netuid]?.takeIf { System.nanoTime() < it.deadlineNanos }
        if (freshEntry != null) return@withLock freshEntry.data
        val response = api.validators(url)
        val cached = readCacheEntry(response)
        requireNotNull(cached.data.items) { "Missing validator directory" }
        if (lists.size >= 64) lists.clear()
        lists[netuid] = cached
        cached.data
    }

    suspend fun gates(): BittensorValidatorGates = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val url = endpoint(baseUrl, "recommendations/subnets")
        val cached = recommendations?.takeIf { System.nanoTime() < it.deadlineNanos }
            ?: loadRecommendations(url)
        val gates = bindBittensorValidatorGates(cached.data)
        recommendations = cached
        gates
    }

    suspend fun recommendedHotkeys(netuid: Int): List<String> = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val url = endpoint(baseUrl, "recommendations")
        val cached = validatorRecommendations?.takeIf { System.nanoTime() < it.deadlineNanos }
            ?: loadRecommendations(url)
        val hotkeys = bindRecommendedHotkeys(cached.data, netuid)
        validatorRecommendations = cached
        hotkeys
    }

    private suspend fun loadRecommendations(url: String): Cached<BittensorRecommendationsRemote> {
        val response = api.recommendations(url)
        return readCacheEntry(response)
    }

    private fun bindRecommendedHotkeys(response: BittensorRecommendationsRemote, netuid: Int): List<String> {
        requireUsableBittensorRecommendations(response)
        val sourceBlock = requireNotNull(response.meta?.generation?.sourceBlockNumber) { "Missing recommendation generation" }
        require(sourceBlock.isNonNegative) { "Invalid recommendation block" }
        val classes = requireNotNull(response.classes) { "Missing validator recommendations" }
        return listOf(classes.stable, classes.balanced, classes.higherUpside)
            .flatMap { it.orEmpty() }
            .filter { it.netuid == netuid }
            .mapNotNull { it.hotkey }
            .distinct()
    }

    private suspend fun refreshInfrastructure(): HttpUrl {
        val base = infrastructureUrls.baseUrl() ?: throw InfrastructureUrlMissingException()
        require(base.isHttps) { "Infrastructure requires HTTPS" }
        if (base != cachedBaseUrl) {
            lists.clear()
            recommendations = null
            validatorRecommendations = null
            cachedBaseUrl = base
        }
        return base
    }

    private fun endpoint(baseUrl: HttpUrl, path: String): String = requireNotNull(baseUrl.resolve("/v1/bittensor/$path"))
        .toString()

    private fun <T> readCacheEntry(response: Response<T>): Cached<T> {
        if (!response.isSuccessful) throw HttpException(response)
        val lifetimeNanos = cacheLifetimeNanos(response)
        return Cached(
            requireNotNull(response.body()) { "Empty Bittensor response" },
            System.nanoTime() + lifetimeNanos
        )
    }

    private fun cacheLifetimeNanos(response: Response<*>): Long {
        val control = CacheControl.parse(response.headers())
        val maxAge = if (control.noStore || control.noCache) 0 else control.maxAgeSeconds.coerceIn(0, 300)
        val age = response.headers()["Age"]
            ?.toIntOrNull()
            ?.coerceAtLeast(0) ?: 0
        return (maxAge - age).coerceAtLeast(0) * 1_000_000_000L
    }
}
