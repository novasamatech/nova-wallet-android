package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

private const val MARKET_PAGE_SIZE = 250

private const val MAX_MARKET_PAGES = 4

private val MARKET_CACHE_VALIDITY = 5.minutes

private val SUBNET_SYMBOL_PATTERN = Regex("sn[0-9]+")

private val MARKET_ID_PATTERN = Regex("[a-z0-9-]+")

/** CoinGecko uses sn<N> market symbols; names and alpha symbols are not reliable identifiers. */
class BittensorSubnetMarketRepository(private val api: BittensorSubnetMarketsApi) {

    private val mutex = Mutex()

    private var cached = emptyMap<BittensorNetuid.Subnet, String>()

    private var cachedMarkets = emptyMap<BittensorNetuid.Subnet, BittensorSubnetMarketRemote>()

    private var loadedAt = 0L

    suspend fun priceId(subnetId: BittensorNetuid.Subnet): String? = mutex.withLock {
        refreshMarkets()
        cached[subnetId]
    }

    suspend fun weeklyMarkets(): Map<BittensorNetuid.Subnet, BittensorSubnetMarketRemote> = mutex.withLock {
        refreshMarkets()
        cachedMarkets
    }

    private suspend fun refreshMarkets() {
        if (loadedAt == 0L || System.nanoTime() - loadedAt > MARKET_CACHE_VALIDITY.inWholeNanoseconds) {
            val requestStartedAt = System.nanoTime()
            val markets = loadMarkets()
            cached = mapBittensorMarketIds(markets)
            val byId = markets.associateBy { it.id }
            cachedMarkets = cached.mapValues { (_, id) -> requireNotNull(byId[id]) }
            loadedAt = requestStartedAt
        }
    }

    private suspend fun loadMarkets(): List<BittensorSubnetMarketRemote> = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
        buildList {
            for (page in 1..MAX_MARKET_PAGES) {
                val markets = api.markets(perPage = MARKET_PAGE_SIZE, page = page)
                addAll(markets)
                if (markets.size < MARKET_PAGE_SIZE) break
            }
        }
    } ?: error("Subnet markets timed out")
}

internal fun mapBittensorMarketIds(rows: List<BittensorSubnetMarketRemote>): Map<BittensorNetuid.Subnet, String> = buildMap {
    rows.forEach { row ->
        val symbol = row.symbol?.trim()?.lowercase() ?: return@forEach
        if (!symbol.matches(SUBNET_SYMBOL_PATTERN)) return@forEach
        val netuid = symbol.drop(2)
            .toIntOrNull() ?: return@forEach
        val subnetId = runCatching { BittensorNetuid.Subnet(netuid) }
            .getOrNull() ?: return@forEach
        val id = row.id?.trim()?.takeIf { it.matches(MARKET_ID_PATTERN) } ?: return@forEach
        if (subnetId !in this) put(subnetId, id)
    }
}
