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
class BittensorSubnetMarketRepository(
    private val api: BittensorSubnetMarketsApi,
    private val nanoTime: () -> Long = System::nanoTime
) {

    private class CachedMarkets(val rows: Map<BittensorNetuid.Subnet, BittensorSubnetMarketRemote>, val loadedAtNanos: Long)

    private val mutex = Mutex()

    private var cached: CachedMarkets? = null

    suspend fun priceId(subnetId: BittensorNetuid.Subnet): String? = mutex.withLock {
        markets()[subnetId]?.id
    }

    suspend fun weeklyMarkets(): Map<BittensorNetuid.Subnet, BittensorSubnetMarketRemote> = mutex.withLock {
        markets()
    }

    private suspend fun markets(): Map<BittensorNetuid.Subnet, BittensorSubnetMarketRemote> {
        cached?.takeIf { nanoTime() - it.loadedAtNanos < MARKET_CACHE_VALIDITY.inWholeNanoseconds }?.let { return it.rows }

        val requestStartedAt = nanoTime()
        val response = loadMarkets()
        val markets = mapBittensorMarkets(response)
        // No listed subnet is a degraded response, not every subnet unlisted, so the read fails.
        require(markets.isNotEmpty()) { "Empty subnet markets" }
        cached = CachedMarkets(markets, requestStartedAt)
        return markets
    }

    private suspend fun loadMarkets(): List<BittensorSubnetMarketRemote> = withTimeoutOrNull(BITTENSOR_REQUEST_TIMEOUT) {
        val markets = mutableListOf<BittensorSubnetMarketRemote>()
        val seenMarketIds = mutableSetOf<String>()
        for (page in 1..MAX_MARKET_PAGES) {
            val response = api.markets(perPage = MARKET_PAGE_SIZE, page = page)
            require(response.size <= MARKET_PAGE_SIZE) { "Oversized subnet markets page" }
            val pageIds = response.mapNotNull { it.id?.trim() }
            require(pageIds.all { seenMarketIds.add(it) }) { "Duplicate subnet market id" }
            markets.addAll(response)
            if (response.size < MARKET_PAGE_SIZE) return@withTimeoutOrNull markets
        }
        error("Incomplete subnet markets pagination")
    } ?: throw BittensorTimeoutException("Subnet markets")
}

internal fun mapBittensorMarketIds(rows: List<BittensorSubnetMarketRemote>): Map<BittensorNetuid.Subnet, String> =
    mapBittensorMarkets(rows).mapValues { (_, market) -> requireNotNull(market.id) }

private fun mapBittensorMarkets(rows: List<BittensorSubnetMarketRemote>): Map<BittensorNetuid.Subnet, BittensorSubnetMarketRemote> = buildMap {
    rows.forEach { row ->
        val symbol = row.symbol?.trim()?.lowercase() ?: return@forEach
        if (!symbol.matches(SUBNET_SYMBOL_PATTERN)) return@forEach
        val netuid = symbol.drop(2)
            .toIntOrNull() ?: return@forEach
        val subnetId = runCatching { BittensorNetuid.Subnet(netuid) }
            .getOrNull() ?: return@forEach
        val id = row.id?.trim()?.takeIf { it.matches(MARKET_ID_PATTERN) } ?: return@forEach
        if (subnetId !in this) put(subnetId, row.copy(id = id))
    }
}
