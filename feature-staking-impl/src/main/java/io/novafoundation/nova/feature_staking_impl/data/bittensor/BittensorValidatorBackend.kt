package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import java.math.BigDecimal
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.CacheControl
import okhttp3.HttpUrl
import retrofit2.HttpException
import retrofit2.Response

/** Only HTTP-fresh data is reused; stale data never supplies a fallback eligibility policy. */
class BittensorValidatorBackend(
    private val api: BittensorValidatorsApi,
    private val infrastructureUrls: InfrastructureUrls,
    private val nanoTime: () -> Long = System::nanoTime
) {

    private class Cached<T>(val data: T, val deadlineNanos: Long)

    private val mutex = Mutex()

    private var cachedBaseUrl: HttpUrl? = null

    private val lists = mutableMapOf<Int, Cached<BittensorValidatorsRemote>>()

    private var rootValidators: Cached<BittensorRootValidatorRemote>? = null

    private var rootOverview: Cached<BittensorRootOverviewRemote>? = null

    private val ranked = mutableMapOf<Int, Cached<BittensorRankedValidatorsRemote>>()

    private var recommendations: Cached<BittensorRecommendationsRemote>? = null

    private var validatorRecommendations: Cached<BittensorRecommendationsRemote>? = null

    private var newestGeneration: BittensorRecommendationGeneration? = null

    suspend fun validators(netuid: Int): BittensorValidatorsRemote = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val url = endpoint(baseUrl, "subnets/$netuid/validators")
        val freshEntry = lists[netuid]?.takeIf { nanoTime() < it.deadlineNanos }
        if (freshEntry != null) return@withLock freshEntry.data
        val startedAt = nanoTime()
        val response = api.validators(url)
        val cached = readCacheEntry(response, startedAt)
        requireNotNull(cached.data.items) { "Missing validator directory" }
        if (lists.size >= 64) lists.clear()
        lists[netuid] = cached
        cached.data
    }

    suspend fun rootValidators(): BittensorRootValidatorRemote = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val freshEntry = rootValidators?.takeIf { nanoTime() < it.deadlineNanos }
        if (freshEntry != null) return@withLock freshEntry.data
        val startedAt = nanoTime()
        val response = api.rootValidators(endpoint(baseUrl, "root/validators"))
        val cached = readCacheEntry(response, startedAt)
        requireFreshRootComponent(cached.data.meta?.components?.rootValidators)
        requireFreshRootComponent(cached.data.meta?.components?.stakeYieldWindows)
        requireNotNull(cached.data.items).forEach { row ->
            row.rootStakeTao?.toBigDecimal()
            row.stakeYield?.d30?.let { requireNotNull(it.apy).toBigDecimal() }
        }
        rootValidators = cached
        cached.data
    }

    /** Annual fraction (8.5 reported percent becomes 0.085), matching the network-rate consumer contract. */
    suspend fun networkRootRate(): BigDecimal? = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val cached = rootOverview?.takeIf { nanoTime() < it.deadlineNanos } ?: run {
            val startedAt = nanoTime()
            val response = api.rootOverview(endpoint(baseUrl, "root/overview"))
            readCacheEntry(response, startedAt)
        }
        requireFreshRootComponent(cached.data.meta?.components?.indexes)
        val rate = cached.data.stakeIndex?.stakeYield?.d30?.let { requireNotNull(it.apy).toBigDecimal() }
        rootOverview = cached
        rate?.takeIf { it.isNonNegative }?.movePointLeft(2)
    }

    private fun requireFreshRootComponent(component: BittensorRecommendationsRemote.Component?) {
        val metadata = requireNotNull(component) { "Missing root yield metadata" }
        require(metadata.availability == "AVAILABLE" && metadata.freshness == "FRESH") { "Root yield data is unavailable or stale" }
        parseBittensorTimestamp(requireNotNull(metadata.asOf))
    }

    suspend fun rankedValidators(netuid: Int): BittensorRankedValidatorsRemote = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        BittensorNetuid.from(netuid)
        val freshEntry = ranked[netuid]?.takeIf { nanoTime() < it.deadlineNanos }
        if (freshEntry != null) return@withLock freshEntry.data
        val startedAt = nanoTime()
        val response = api.rankedValidators(endpoint(baseUrl, "recommendations/subnets/$netuid/validators"))
        val cached = readCacheEntry(response, startedAt)
        mapBittensorValidatorGates(BittensorRecommendationsRemote(cached.data.meta))
        requireNotNull(cached.data.items)
        admitGeneration(cached.data.meta)
        if (ranked.size >= 64) ranked.clear()
        ranked[netuid] = cached
        cached.data
    }

    suspend fun gates(): BittensorValidatorGates = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val url = endpoint(baseUrl, "recommendations/subnets")
        val cached = recommendations?.takeIf { nanoTime() < it.deadlineNanos }
            ?: loadRecommendations(url)
        val gates = mapBittensorValidatorGates(cached.data)
        admitGeneration(cached.data.meta)
        recommendations = cached
        gates
    }

    suspend fun rankedSubnet(subnet: BittensorNetuid.Subnet, registrationBlock: java.math.BigInteger?): BittensorSubnetRankingRemote? = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val cached = recommendations?.takeIf { nanoTime() < it.deadlineNanos }
            ?: loadRecommendations(endpoint(baseUrl, "recommendations/subnets"))
        requireUsableBittensorRecommendations(cached.data)
        val sourceBlock = requireNotNull(cached.data.meta?.generation?.sourceBlockNumber)
        require(registrationBlock == null || sourceBlock >= registrationBlock) { "Subnet ranking predates registration" }
        val items = requireNotNull(cached.data.items) { "Missing subnet rankings" }
        require(items.map { it.netuid }.distinct().size == items.size) { "Duplicate subnet rankings" }
        admitGeneration(cached.data.meta)
        recommendations = cached
        items.singleOrNull { it.netuid == subnet.value }
    }

    suspend fun recommendedHotkeys(netuid: Int): List<String> {
        val recommendations = recommendedValidators()
        return recommendations
            .filter { it.netuid == netuid }
            .mapNotNull { it.hotkey }
            .distinct()
    }

    suspend fun recommendedValidators(): List<BittensorRecommendationsRemote.Item> = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val url = endpoint(baseUrl, "recommendations")
        val cached = validatorRecommendations?.takeIf { nanoTime() < it.deadlineNanos }
            ?: loadRecommendations(url)
        val validators = mapRecommendedValidators(cached.data)
        admitGeneration(cached.data.meta)
        validatorRecommendations = cached
        validators
    }

    private suspend fun loadRecommendations(url: String): Cached<BittensorRecommendationsRemote> {
        val startedAt = nanoTime()
        val response = api.recommendations(url)
        return readCacheEntry(response, startedAt)
    }

    // Called under mutex, only after the response has passed semantic validation.
    private fun admitGeneration(meta: BittensorRecommendationsRemote.Meta?) {
        val generation = mapBittensorRecommendationGeneration(meta)
        val newest = newestGeneration
        require(newest == null || generation >= newest) { "Recommendation generation is stale" }
        if (newest != null && generation > newest) {
            ranked.clear()
            recommendations = null
            validatorRecommendations = null
        }
        newestGeneration = generation
    }

    private fun mapRecommendedValidators(response: BittensorRecommendationsRemote): List<BittensorRecommendationsRemote.Item> {
        requireUsableBittensorRecommendations(response)
        val sourceBlock = requireNotNull(response.meta?.generation?.sourceBlockNumber) { "Missing recommendation generation" }
        require(sourceBlock.isNonNegative) { "Invalid recommendation block" }
        val classes = requireNotNull(response.classes) { "Missing validator recommendations" }
        return listOf(classes.stable, classes.balanced, classes.higherUpside)
            .flatMap { it.orEmpty() }
    }

    private suspend fun refreshInfrastructure(): HttpUrl {
        val base = infrastructureUrls.baseUrl() ?: throw InfrastructureUrlMissingException()
        require(base.isHttps) { "Infrastructure requires HTTPS" }
        if (base != cachedBaseUrl) {
            lists.clear()
            rootValidators = null
            rootOverview = null
            ranked.clear()
            recommendations = null
            validatorRecommendations = null
            newestGeneration = null
            cachedBaseUrl = base
        }
        return base
    }

    private fun endpoint(baseUrl: HttpUrl, path: String): String = bittensorBackendUrl(baseUrl, path)

    private fun <T> readCacheEntry(response: Response<T>, startedAt: Long): Cached<T> {
        if (!response.isSuccessful) throw HttpException(response)
        val lifetimeNanos = cacheLifetimeNanos(response)
        return Cached(
            requireNotNull(response.body()) { "Empty Bittensor response" },
            startedAt + lifetimeNanos
        )
    }

    private fun cacheLifetimeNanos(response: Response<*>): Long {
        val control = CacheControl.parse(response.headers())
        val maxAge = if (control.noStore || control.noCache) 0 else control.maxAgeSeconds.coerceIn(0, 300)
        val age = response.headers()["Age"]
            ?.toIntOrNull()
            ?.coerceAtLeast(0) ?: 0
        return (maxAge - age)
            .coerceAtLeast(0) * 1_000_000_000L
    }
}
