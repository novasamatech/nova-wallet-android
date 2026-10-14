package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.time.Duration.Companion.minutes

private const val MARKET_PAGE_SIZE = 250

private const val MAX_MARKET_PAGES = 4

private val MARKET_CACHE_VALIDITY = 5.minutes

private val SUBNET_SYMBOL_PATTERN = Regex("sn[0-9]+")

private val MARKET_ID_PATTERN = Regex("[a-z0-9-]+")

interface BittensorSubnetMarketsApi {
    @GET("/api/v3/coins/markets")
    suspend fun markets(
        @Query("category") category: String = "bittensor-subnets",
        @Query("vs_currency") currency: String = "usd",
        @Query("per_page") perPage: Int = MARKET_PAGE_SIZE,
        @Query("page") page: Int
    ): List<BittensorSubnetMarketRemote>
}

data class BittensorSubnetMarketRemote(val id: String?, val symbol: String?)

/** CoinGecko uses sn<N> market symbols; names and alpha symbols are not reliable identifiers. */
class BittensorSubnetMarketRepository(private val api: BittensorSubnetMarketsApi) {

    private val mutex = Mutex()

    private var cached = emptyMap<BittensorSubnetId, String>()

    private var loadedAt = 0L

    suspend fun priceId(subnetId: BittensorSubnetId): String? = mutex.withLock {
        if (loadedAt == 0L || System.nanoTime() - loadedAt > MARKET_CACHE_VALIDITY.inWholeNanoseconds) {
            cached = bindBittensorMarketIds(loadMarkets())
            loadedAt = System.nanoTime()
        }
        cached[subnetId]
    }

    private suspend fun loadMarkets(): List<BittensorSubnetMarketRemote> = withTimeout(20_000L) {
        buildList {
            for (page in 1..MAX_MARKET_PAGES) {
                val markets = api.markets(page = page)
                addAll(markets)
                if (markets.size < MARKET_PAGE_SIZE) break
            }
        }
    }
}

internal fun bindBittensorMarketIds(rows: List<BittensorSubnetMarketRemote>): Map<BittensorSubnetId, String> = buildMap {
    rows.forEach { row ->
        val symbol = row.symbol?.trim()?.lowercase() ?: return@forEach
        if (!symbol.matches(SUBNET_SYMBOL_PATTERN)) return@forEach
        val netuid = symbol.drop(2).toIntOrNull()?.takeIf { it in 1..65535 } ?: return@forEach
        val id = row.id?.trim()?.takeIf { it.matches(MARKET_ID_PATTERN) } ?: return@forEach
        val subnetId = BittensorSubnetId(netuid)
        if (subnetId !in this) put(subnetId, id)
    }
}
