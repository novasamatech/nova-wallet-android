package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** [pricesPending] stays true until a price request for the held subnets completes, even unsuccessfully. */
data class BittensorPortfolioState(
    val positions: BittensorPortfolioPositions?,
    val status: BittensorDataStatus,
    val pricesPending: Boolean = false
)

interface BittensorPositionRepository {

    suspend fun invalidate(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): Result<Unit>

    fun observePositions(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): Flow<BittensorPositionState>

    fun observePortfolio(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): Flow<BittensorPortfolioState>
}

class RealBittensorPositionRepository(
    private val positionSource: BittensorPositionSource,
    private val pricesSource: BittensorPricesSource
) : BittensorPositionRepository {

    override fun observePositions(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): Flow<BittensorPositionState> =
        positionSource.observePositions(chainId, coldKey, metaAccountId)

    override fun observePortfolio(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): Flow<BittensorPortfolioState> {
        val positions = positionSource.observePositions(chainId, coldKey, metaAccountId)
        val prices = pricesSource.observePrices(chainId, coldKey, positions, metaAccountId)
        return combine(positions, prices, ::valueLatestPositions)
    }

    override suspend fun invalidate(chainId: ChainId, coldKey: AccountId, metaAccountId: Long): Result<Unit> {
        val positionsResult = runCatching {
            positionSource.invalidate(chainId, coldKey, metaAccountId)
        }.onFailure { if (it is CancellationException) throw it }

        // Both independent sources must be invalidated even if the first one fails.
        val pricesResult = runCatching {
            pricesSource.invalidate(chainId, coldKey, metaAccountId)
        }.onFailure { if (it is CancellationException) throw it }

        val positionsError = positionsResult.exceptionOrNull()
        val pricesError = pricesResult.exceptionOrNull()
        if (positionsError != null && pricesError != null && positionsError !== pricesError) {
            positionsError.addSuppressed(pricesError)
        }
        return if (positionsResult.isFailure) positionsResult else pricesResult
    }

    private fun valueLatestPositions(state: BittensorPositionState, prices: BittensorPortfolioPrices): BittensorPortfolioState {
        val heldSubnets = state.positions.heldSubnets()
        val matching = heldSubnets == prices.heldSubnets
        val positions = state.positions?.let { mapBittensorPortfolio(it, if (matching) prices.prices else emptyMap()) }
        val pricesPending = state.status !is BittensorDataStatus.Stale && heldSubnets.isNotEmpty() && !(matching && prices.loaded)
        val status = if (matching && prices.stale && heldSubnets.isNotEmpty() && state.status is BittensorDataStatus.Fresh) {
            BittensorDataStatus.Stale(BittensorCacheStaleException())
        } else {
            state.status
        }
        return BittensorPortfolioState(positions, status, pricesPending)
    }
}
