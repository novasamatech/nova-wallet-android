package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import retrofit2.http.GET
import retrofit2.http.Query

interface BittensorSubnetMarketsApi {
    @GET("/api/v3/coins/markets")
    suspend fun markets(
        @Query("category") category: String = "bittensor-subnets",
        @Query("vs_currency") currency: String = "usd",
        @Query("per_page") perPage: Int = 250,
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
        if (System.nanoTime() - loadedAt > 300_000_000_000L || loadedAt == 0L) {
            val markets = withTimeout(20_000L) {
                buildList {
                    for (page in 1..4) {
                        val rows = api.markets(page = page)
                        addAll(rows)
                        if (rows.size < 250) break
                    }
                }
            }
            cached = bindBittensorMarketIds(markets)
            loadedAt = System.nanoTime()
        }
        cached[subnetId]
    }
}

internal fun bindBittensorMarketIds(rows: List<BittensorSubnetMarketRemote>): Map<BittensorSubnetId, String> = buildMap {
    rows.forEach { row ->
        val symbol = row.symbol?.trim()?.lowercase() ?: return@forEach
        if (!symbol.matches(Regex("sn[0-9]+"))) return@forEach
        val netuid = symbol.drop(2).toIntOrNull()?.takeIf { it in 1..65535 } ?: return@forEach
        val id = row.id?.trim()?.takeIf { it.matches(Regex("[a-z0-9-]+")) } ?: return@forEach
        putIfAbsent(BittensorSubnetId(netuid), id)
    }
}
