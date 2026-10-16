package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorGates

import okhttp3.CacheControl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
    suspend fun rootClaims(@Url url: String): Response<BittensorRootClaimsRemote>

    @GET
    suspend fun rootValidator(@Url url: String): Response<BittensorRootValidatorRemote>

    @GET
    suspend fun rankedValidators(@Url url: String): Response<BittensorRankedValidatorsRemote>

    @GET
    suspend fun recommendations(@Url url: String): Response<BittensorRecommendationsRemote>
}

class BittensorValidatorsRemote(val items: List<Item>?) {
    class Item(val netuid: Int?, val hotkey: String?, val identitySourceName: String?, val stakeSourceName: String?)
}

class BittensorRecommendationsRemote(val meta: Meta?) {
    class Meta(val clientGates: Gates?, val components: Components?)
    class Components(val recommendations: Component?)
    class Component(val availability: String?, val freshness: String?)
    class Gates(val maxTake: String?, val requirePermit: Boolean?, val requireActiveWithinCutoff: Boolean?)
}

/** Only HTTP-fresh data is reused; stale data never supplies a fallback eligibility policy. */
class BittensorValidatorBackend(private val api: BittensorValidatorsApi, private val baseUrl: String) {

    private class Cached<T>(val data: T, val deadlineNanos: Long)
    private val mutex = Mutex()
    private val lists = mutableMapOf<Int, Cached<BittensorValidatorsRemote>>()
    private val rootValidators = mutableMapOf<String, Cached<BittensorRootValidatorRemote>>()
    private val ranked = mutableMapOf<Int, Cached<BittensorRankedValidatorsRemote>>()
    private var recommendations: Cached<BittensorRecommendationsRemote>? = null

    suspend fun validators(netuid: Int): BittensorValidatorsRemote = mutex.withLock {
        lists[netuid]?.takeIf { System.nanoTime() < it.deadlineNanos }?.let { return@withLock it.data }
        val response = api.validators(url("subnets/$netuid/validators"))
        val cached = cache(response)
        if (lists.size >= 64) lists.clear()
        lists[netuid] = cached
        cached.data
    }

    private data class ClaimEventId(val block: Long, val extrinsic: Int, val event: Int)

    suspend fun receivedRootRewards(accountSubject: String): java.math.BigDecimal {
        val target = requireNotNull(url("root/claims/search").toHttpUrlOrNull()).newBuilder()
            .addQueryParameter("accountSubject", accountSubject).addQueryParameter("page", "1").build()
        val response = api.rootClaims(target.toString())
        if (!response.isSuccessful) throw HttpException(response)
        val body = requireNotNull(response.body())
        require(body.historyScope == "TAO_APP_PARTIAL")
        val component = requireNotNull(body.meta?.components?.rootClaims)
        require(component.availability == "AVAILABLE" && component.freshness == "FRESH")
        val seen = mutableSetOf<ClaimEventId>()
        return requireNotNull(body.items).fold(java.math.BigDecimal.ZERO) { total, row ->
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
        require(hotkey.length == 48 && hotkey.all { it in "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz" })
        val cached = rootValidators[hotkey]?.takeIf { System.nanoTime() < it.deadlineNanos }
            ?: cache(api.rootValidator(url("root/validators/$hotkey"))).also {
                if (rootValidators.size >= 64) rootValidators.clear()
                rootValidators[hotkey] = it
            }
        val component = requireNotNull(cached.data.meta?.components?.rootValidator)
        require(component.availability == "AVAILABLE" && component.freshness == "FRESH") { "Root validator data is unavailable or stale" }
        require(cached.data.validator?.hotkey == hotkey)
        cached.data
    }

    suspend fun rankedValidators(netuid: Int): BittensorRankedValidatorsRemote = mutex.withLock {
        require(netuid in 0..65535)
        val cached = ranked[netuid]?.takeIf { System.nanoTime() < it.deadlineNanos }
            ?: cache(api.rankedValidators(url("recommendations/subnets/$netuid/validators"))).also {
                if (ranked.size >= 64) ranked.clear()
                ranked[netuid] = it
            }
        bindBittensorValidatorGates(BittensorRecommendationsRemote(cached.data.meta))
        requireNotNull(cached.data.items)
        cached.data
    }

    suspend fun gates(): BittensorValidatorGates = mutex.withLock {
        val cached = recommendations?.takeIf { System.nanoTime() < it.deadlineNanos }
            ?: cache(api.recommendations(url("recommendations/subnets"))).also { recommendations = it }
        bindBittensorValidatorGates(cached.data)
    }

    private fun url(path: String): String {
        val base = requireNotNull(baseUrl.toHttpUrlOrNull()) { "Infrastructure URL is not configured" }
        require(base.isHttps) { "Infrastructure requires HTTPS" }
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
