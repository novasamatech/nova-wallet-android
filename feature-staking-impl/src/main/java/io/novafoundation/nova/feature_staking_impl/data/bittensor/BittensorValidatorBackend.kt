package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.HttpException
import retrofit2.Response

/** Only HTTP-fresh data is reused; stale data never supplies a fallback eligibility policy. */
class BittensorValidatorBackend(private val api: BittensorValidatorsApi, private val infrastructureUrls: InfrastructureUrls) {

    private class Cached<T>(val data: T, val deadlineNanos: Long)

    private val mutex = Mutex()

    private var cachedBaseUrl: HttpUrl? = null

    private val lists = mutableMapOf<Int, Cached<BittensorValidatorsRemote>>()

    private val rootValidators = mutableMapOf<String, Cached<BittensorRootValidatorRemote>>()

    private val ranked = mutableMapOf<Int, Cached<BittensorRankedValidatorsRemote>>()

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

    private data class ClaimEventId(val block: Long, val extrinsic: Int, val event: Int)

    suspend fun receivedRootRewards(accountSubject: String): java.math.BigDecimal = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val target = requireNotNull(endpoint(baseUrl, "root/claims/search").toHttpUrlOrNull()).newBuilder()
            .addQueryParameter("accountSubject", accountSubject).addQueryParameter("page", "1").build()
        val response = api.rootClaims(target.toString())
        if (!response.isSuccessful) throw HttpException(response)
        val body = requireNotNull(response.body())
        require(body.historyScope == "TAO_APP_PARTIAL")
        val component = requireNotNull(body.meta?.components?.rootClaims)
        require(component.availability == "AVAILABLE" && component.freshness == "FRESH")
        val seen = mutableSetOf<ClaimEventId>()
        requireNotNull(body.items).fold(java.math.BigDecimal.ZERO) { total, row ->
            val block = requireNotNull(row.block)
            val extrinsic = requireNotNull(row.extrinsicIndex)
            val event = requireNotNull(row.eventIndex)
            require(block >= 0 && extrinsic >= 0 && event >= 0)
            val amount = requireNotNull(row.payoutTao).toBigDecimal()
            require(amount.isNonNegative)
            if (seen.add(ClaimEventId(block, extrinsic, event))) total + amount else total
        }
    }

    suspend fun rootValidator(hotkey: String): BittensorRootValidatorRemote = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        require(hotkey.length == 48 && hotkey.all { it in "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz" })
        val cached = rootValidators[hotkey]?.takeIf { System.nanoTime() < it.deadlineNanos }
            ?: readCacheEntry(api.rootValidator(endpoint(baseUrl, "root/validators/$hotkey"))).also {
                if (rootValidators.size >= 64) rootValidators.clear()
                rootValidators[hotkey] = it
            }
        val component = requireNotNull(cached.data.meta?.components?.rootValidator)
        require(component.availability == "AVAILABLE" && component.freshness == "FRESH") { "Root validator data is unavailable or stale" }
        require(cached.data.validator?.hotkey == hotkey)
        cached.data
    }

    suspend fun rankedValidators(netuid: Int): BittensorRankedValidatorsRemote = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        require(netuid in 0..65535)
        val cached = ranked[netuid]?.takeIf { System.nanoTime() < it.deadlineNanos }
            ?: readCacheEntry(api.rankedValidators(endpoint(baseUrl, "recommendations/subnets/$netuid/validators"))).also {
                if (ranked.size >= 64) ranked.clear()
                ranked[netuid] = it
            }
        mapBittensorValidatorGates(BittensorRecommendationsRemote(cached.data.meta))
        requireNotNull(cached.data.items)
        cached.data
    }

    suspend fun gates(): BittensorValidatorGates = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val url = endpoint(baseUrl, "recommendations/subnets")
        val cached = recommendations?.takeIf { System.nanoTime() < it.deadlineNanos }
            ?: loadRecommendations(url)
        val gates = mapBittensorValidatorGates(cached.data)
        recommendations = cached
        gates
    }

    suspend fun recommendedHotkeys(netuid: Int): List<String> = mutex.withLock {
        val baseUrl = refreshInfrastructure()
        val url = endpoint(baseUrl, "recommendations")
        val cached = validatorRecommendations?.takeIf { System.nanoTime() < it.deadlineNanos }
            ?: loadRecommendations(url)
        val hotkeys = mapRecommendedHotkeys(cached.data, netuid)
        validatorRecommendations = cached
        hotkeys
    }

    private suspend fun loadRecommendations(url: String): Cached<BittensorRecommendationsRemote> {
        val response = api.recommendations(url)
        return readCacheEntry(response)
    }

    private fun mapRecommendedHotkeys(response: BittensorRecommendationsRemote, netuid: Int): List<String> {
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
            rootValidators.clear()
            ranked.clear()
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
