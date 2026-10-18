package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BittensorAlphaYieldRepository(
    private val remoteSource: BittensorEstimatesRemoteSource,
    private val infrastructureUrls: InfrastructureUrls
) {

    private data class CachedYields(val yields: Map<AccountIdKey, BigDecimal>, val validUntilNanos: Long)

    private val mutex = Mutex()

    private val yields = mutableMapOf<String, CachedYields>()

    /** The reported rate is an annual percent; validator commission is already accounted for. */
    suspend fun alphaYields(chain: Chain, subnet: BittensorNetuid.Subnet): Map<AccountIdKey, BigDecimal> = mutex.withLock {
        val baseUrl = requireNotNull(infrastructureUrls.baseUrl())
        val cacheKey = "$baseUrl:${subnet.value}"
        val cached = yields[cacheKey]
        if (cached != null && System.nanoTime() < cached.validUntilNanos) return@withLock cached.yields

        val pages = remoteSource.yields(baseUrl, chain, subnet)
        val rates = buildMap {
            pages.items.forEach { yield ->
                if (yield.hotkey !in this) put(yield.hotkey, yield.annualPercentageYield)
            }
        }
        if (yields.size >= YIELD_CACHE_LIMIT) yields.clear()
        yields[cacheKey] = CachedYields(rates, pages.validUntilNanos)
        rates
    }

    private companion object {
        const val YIELD_CACHE_LIMIT = 64
    }
}
