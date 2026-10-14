package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls

import okhttp3.CacheControl
import okhttp3.HttpUrl
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Url
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface BittensorValidatorsApi {

    @GET
    suspend fun validators(@Url url: String): Response<BittensorValidatorsRemote>

    @GET
    suspend fun recommendations(@Url url: String): Response<BittensorRecommendationsRemote>
}

class BittensorValidatorsRemote(val items: List<Item>?) {
    class Item(val netuid: Int?, val hotkey: String?, val identitySourceName: String?, val stakeSourceName: String?)
}

class BittensorRecommendationsRemote(val meta: Meta?, val classes: Classes? = null) {
    class Meta(val clientGates: Gates?, val components: Components?, val generation: Generation? = null)
    class Generation(val sourceBlockNumber: java.math.BigInteger?)
    class Classes(val stable: List<Item>?, val balanced: List<Item>?, val higherUpside: List<Item>?)
    class Item(val netuid: Int?, val hotkey: String?)
    class Components(val recommendations: Component?)
    class Component(val availability: String?, val freshness: String?)
    class Gates(val maxTake: String?, val requirePermit: Boolean?, val requireActiveWithinCutoff: Boolean?)
}

/** Only HTTP-fresh data is reused; stale data never supplies a fallback eligibility policy. */
class BittensorValidatorBackend(private val api: BittensorValidatorsApi, private val infrastructureUrls: InfrastructureUrls) {

    private class Cached<T>(val data: T, val deadlineNanos: Long)

    private val mutex = Mutex()

    private var cachedBaseUrl: HttpUrl? = null

    private val lists = mutableMapOf<Int, Cached<BittensorValidatorsRemote>>()

    private var recommendations: Cached<BittensorRecommendationsRemote>? = null

    private var validatorRecommendations: Cached<BittensorRecommendationsRemote>? = null

    suspend fun validators(netuid: Int): BittensorValidatorsRemote = mutex.withLock {
        val url = url("subnets/$netuid/validators")
        lists[netuid]?.takeIf { System.nanoTime() < it.deadlineNanos }?.let { return@withLock it.data }
        val response = api.validators(url)
        val cached = cache(response)
        if (lists.size >= 64) lists.clear()
        lists[netuid] = cached
        cached.data
    }

    suspend fun gates(): BittensorValidatorGates = mutex.withLock {
        val url = url("recommendations/subnets")
        val cached = recommendations?.takeIf { System.nanoTime() < it.deadlineNanos }
            ?: cache(api.recommendations(url)).also { recommendations = it }
        bindBittensorValidatorGates(cached.data)
    }

    suspend fun recommendedHotkeys(netuid: Int): List<String> = mutex.withLock {
        val url = url("recommendations")
        val cached = validatorRecommendations?.takeIf { System.nanoTime() < it.deadlineNanos }
            ?: cache(api.recommendations(url)).also { validatorRecommendations = it }
        bindBittensorValidatorGates(cached.data)
        val sourceBlock = requireNotNull(cached.data.meta?.generation?.sourceBlockNumber) { "Missing recommendation generation" }
        require(sourceBlock.signum() >= 0) { "Invalid recommendation block" }
        val classes = requireNotNull(cached.data.classes) { "Missing validator recommendations" }
        listOf(classes.stable, classes.balanced, classes.higherUpside).flatMap { it.orEmpty() }
            .filter { it.netuid == netuid }.mapNotNull { it.hotkey }.distinct()
    }

    private suspend fun url(path: String): String {
        val base = infrastructureUrls.baseUrl() ?: throw InfrastructureUrlMissingException()
        require(base.isHttps) { "Infrastructure requires HTTPS" }
        if (base != cachedBaseUrl) {
            lists.clear()
            recommendations = null
            validatorRecommendations = null
            cachedBaseUrl = base
        }
        return requireNotNull(base.resolve("/v1/bittensor/$path")).toString()
    }

    private fun <T> cache(response: Response<T>): Cached<T> {
        if (!response.isSuccessful) throw HttpException(response)
        val control = CacheControl.parse(response.headers())
        val maxAge = if (control.noStore || control.noCache) 0 else control.maxAgeSeconds.coerceIn(0, 300)
        val age = response.headers()["Age"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        return Cached(
            requireNotNull(response.body()) { "Empty Bittensor response" },
            System.nanoTime() + (maxAge - age).coerceAtLeast(0) * 1_000_000_000L
        )
    }
}

internal fun bindBittensorValidatorGates(response: BittensorRecommendationsRemote): BittensorValidatorGates {
    val meta = requireNotNull(response.meta) { "Missing validator policy" }
    val component = requireNotNull(meta.components?.recommendations)
    require(component.availability == "AVAILABLE" && component.freshness == "FRESH") { "Validator policy is unavailable or stale" }
    val gates = requireNotNull(meta.clientGates)
    return BittensorValidatorGates(
        requireNotNull(gates.maxTake).toBigDecimal(),
        requireNotNull(gates.requirePermit),
        requireNotNull(gates.requireActiveWithinCutoff)
    )
}
