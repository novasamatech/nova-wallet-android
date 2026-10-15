package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.mapAsync
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

class BittensorPortfolioLoader(
    private val positionSource: BittensorPositionSource,
    private val swapSource: BittensorSwapSource,
    private val logger: Logger
) {

    suspend fun load(chainId: String, coldKey: AccountId, rootOnly: Boolean = false): BittensorPortfolioPositions = withTimeout(POSITIONS_TIMEOUT_MILLIS) {
        val positions = positionSource.positions(chainId, coldKey)
        if (rootOnly) return@withTimeout BittensorPortfolioPositions(positions.root, emptyList(), emptyMap())
        val prices = loadSubnetPrices(chainId, positions.subnets)

        BittensorPortfolioPositions(positions.root, positions.subnets, prices)
    }

    private suspend fun loadSubnetPrices(chainId: String, positions: List<BittensorSubnetPosition>): Map<BittensorNetuid.Subnet, BigInteger> {
        val requests = Semaphore(MAX_CONCURRENT_PRICE_REQUESTS)

        return positions
            .map { it.subnetId }
            .distinct()
            .mapAsync { subnetId ->
                requests.withPermit { loadSubnetPrice(chainId, subnetId) }
            }
            .filterNotNull()
            .toMap()
    }

    private suspend fun loadSubnetPrice(chainId: String, subnetId: BittensorNetuid.Subnet): Pair<BittensorNetuid.Subnet, BigInteger>? {
        return try {
            subnetId to swapSource.currentAlphaPrice(chainId, subnetId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorPortfolio] Failed to load alpha price for subnet ${subnetId.value}")
            logger.log(e)
            null
        }
    }

    private companion object {

        const val POSITIONS_TIMEOUT_MILLIS = 20_000L

        const val MAX_CONCURRENT_PRICE_REQUESTS = 4
    }
}
