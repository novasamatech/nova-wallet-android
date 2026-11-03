package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.memory.observeSharedFlow
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.data.cache.ResponseCache
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import java.math.BigInteger
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

data class BittensorPortfolioPrices(
    val heldSubnets: Set<BittensorNetuid.Subnet>,
    val prices: Map<BittensorNetuid.Subnet, BigInteger>,
    val loaded: Boolean,
    val stale: Boolean = false
)

/** Owns saved portfolio prices, refresh admission and invalidation for each wallet and chain account. */
class BittensorPricesSource(
    private val portfolioLoader: BittensorPortfolioLoader,
    private val cache: ResponseCache,
    private val computationalCache: ComputationalCache
) {

    private data class StoredPrice(val netuid: Int, val price: String)

    private data class StoredPrices(val items: List<StoredPrice>)

    fun observePrices(
        chainId: ChainId,
        coldKey: AccountId,
        positions: Flow<BittensorPositionState>,
        metaAccountId: Long
    ): Flow<BittensorPortfolioPrices> = computationalCache.observeSharedFlow(
        key = BittensorCacheKeys.sharedPrices(chainId, coldKey, metaAccountId)
    ) {
        refreshPrices(chainId, coldKey, positions, metaAccountId)
    }

    private fun refreshPrices(
        chainId: ChainId,
        coldKey: AccountId,
        positions: Flow<BittensorPositionState>,
        metaAccountId: Long
    ): Flow<BittensorPortfolioPrices> = channelFlow {
        val key = BittensorCacheKeys.prices(chainId, coldKey.toHexString())
        var lastPrices = cache.readSnapshot(
            key,
            StoredPrices::class.java,
            metaAccountId
        ) { stored ->
            stored.items.forEach {
                BittensorNetuid.Subnet(it.netuid)
                require(it.price.toBigInteger().isNonNegative)
            }
        }
            ?.items.orEmpty().associate { BittensorNetuid.Subnet(it.netuid) to it.price.toBigInteger() }
        var requestedSubnets = emptySet<BittensorNetuid.Subnet>()
        var priceJob: Job? = null
        send(BittensorPortfolioPrices(emptySet(), emptyMap(), loaded = false))

        positions.collect { state ->
            val heldSubnets = portfolioLoader.heldSubnets(state.positions)
            if (heldSubnets != requestedSubnets) {
                priceJob?.cancel()
                priceJob = null
                requestedSubnets = heldSubnets
                send(
                    BittensorPortfolioPrices(
                        heldSubnets,
                        lastPrices.filterKeys { it in heldSubnets },
                        loaded = lastPrices.keys.containsAll(heldSubnets),
                        stale = lastPrices.isNotEmpty()
                    )
                )
            }

            // Keep an in-flight request for the same held set. Amount/status updates
            // reach the independent combine immediately, without waiting for prices.
            if (state.status is BittensorDataStatus.Fresh && heldSubnets.isNotEmpty() && priceJob?.isActive != true) {
                priceJob = launch {
                    val ticket = cache.snapshotTicket(key, metaAccountId)
                    val prices = portfolioLoader.loadPrices(chainId, heldSubnets)
                    ensureActive()
                    if (prices.keys.containsAll(heldSubnets)) {
                        val stored = StoredPrices(prices.map { (subnet, price) -> StoredPrice(subnet.value, price.toString()) })
                        if (!cache.saveSnapshot(key, metaAccountId, stored, ticket)) return@launch
                    }
                    if (!cache.isSnapshotCurrent(ticket)) return@launch
                    val savedPrices = lastPrices.filterKeys { it in heldSubnets }
                    val usesSavedPrices = savedPrices.keys.any { it !in prices }
                    lastPrices = savedPrices + prices
                    send(BittensorPortfolioPrices(heldSubnets, lastPrices, loaded = true, stale = usesSavedPrices))
                }
            }
        }
    }

    suspend fun invalidate(chainId: ChainId, coldKey: AccountId, metaAccountId: Long) {
        val key = BittensorCacheKeys.prices(chainId, coldKey.toHexString())
        cache.invalidate(keepStale = true) { it.metaAccountId == metaAccountId && it.normalizedUrl == key }
    }
}
