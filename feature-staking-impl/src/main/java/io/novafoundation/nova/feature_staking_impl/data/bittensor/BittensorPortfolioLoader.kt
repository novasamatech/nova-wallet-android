package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakePositions
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

class BittensorPortfolioLoader(
    private val priceRepository: BittensorPortfolioPriceRepository,
    private val logger: Logger
) {

    fun heldSubnets(positions: BittensorStakePositions?): Set<BittensorNetuid.Subnet> = positions?.subnets.orEmpty()
        .filter { it.alphaInPlanks.isPositive() }
        .map { it.subnetId }
        .toSet()

    fun value(positions: BittensorStakePositions, prices: Map<BittensorNetuid.Subnet, BigInteger>): BittensorPortfolioPositions =
        BittensorPortfolioPositions(positions.root, positions.subnets, prices)

    suspend fun loadPrices(chainId: ChainId, heldSubnets: Set<BittensorNetuid.Subnet>): Map<BittensorNetuid.Subnet, BigInteger> {
        if (heldSubnets.isEmpty()) return emptyMap()
        return try {
            val prices = priceRepository.prices(chainId, heldSubnets)
            currentCoroutineContext().ensureActive()
            prices
        } catch (e: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            logger.log("[BittensorPortfolio] Timed out loading subnet prices")
            logger.log(e)
            emptyMap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.log("[BittensorPortfolio] Failed to load portfolio prices")
            logger.log(e)
            emptyMap()
        }
    }
}
