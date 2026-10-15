package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
        val prices = coroutineScope {
            val requests = Semaphore(MAX_CONCURRENT_PRICE_REQUESTS)
            positions.subnets.map { it.subnetId }.distinct().map { subnetId ->
                async {
                    requests.withPermit {
                        try {
                            subnetId to swapSource.currentAlphaPrice(chainId, subnetId)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            logger.log("[BittensorPortfolio] Failed to load alpha price for subnet ${subnetId.value}")
                            logger.log(e)
                            null
                        }
                    }
                }
            }.awaitAll().filterNotNull().toMap()
        }
        BittensorPortfolioPositions(positions.root, positions.subnets, prices)
    }

    private companion object {

        const val POSITIONS_TIMEOUT_MILLIS = 20_000L

        const val MAX_CONCURRENT_PRICE_REQUESTS = 4
    }
}
