package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeout

interface BittensorPortfolioPriceRepository {

    suspend fun prices(chainId: ChainId, heldSubnets: Set<BittensorNetuid.Subnet>): Map<BittensorNetuid.Subnet, BigInteger>
}

class RealBittensorPortfolioPriceRepository(
    private val catalogueRepository: BittensorSubnetCatalogueRepository,
    private val runtimePrices: BittensorRuntimePriceSource,
    private val logger: Logger
) : BittensorPortfolioPriceRepository {

    override suspend fun prices(chainId: ChainId, heldSubnets: Set<BittensorNetuid.Subnet>): Map<BittensorNetuid.Subnet, BigInteger> {
        if (heldSubnets.isEmpty()) return emptyMap()
        val backendPrices = loadPrices(source = "backend") {
            val response = catalogueRepository.catalogue()
            mapBittensorPortfolioPrices(response)
        }
            .filterKeys { it in heldSubnets }
        val missingSubnets = heldSubnets - backendPrices.keys
        if (missingSubnets.isEmpty()) return backendPrices

        val fallbackPrices = loadPrices(source = "runtime") { runtimePrices.prices(chainId) }
            .filterKeys { it in missingSubnets }
        return backendPrices + fallbackPrices
    }

    private suspend fun loadPrices(
        source: String,
        load: suspend () -> Map<BittensorNetuid.Subnet, BigInteger>
    ): Map<BittensorNetuid.Subnet, BigInteger> = try {
        withTimeout(BITTENSOR_REQUEST_TIMEOUT) { load() }
    } catch (e: TimeoutCancellationException) {
        currentCoroutineContext().ensureActive()
        logFailure(source, e)
        emptyMap()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logFailure(source, e)
        emptyMap()
    }

    private fun logFailure(source: String, cause: Throwable) {
        logger.log("[BittensorPortfolio] Failed to load $source prices")
        logger.log(cause)
    }
}

/** Holdings are valued only at FRESH backend prices (iOS `freshTaoPerAlpha`); STALE ones yield to live runtime prices. */
internal fun mapBittensorPortfolioPrices(response: BittensorSubnetCatalogueRemote): Map<BittensorNetuid.Subnet, BigInteger> {
    val isFresh = requireAvailableBittensorComponent(response.meta?.components?.alphaPrices, name = "Portfolio prices")
    return if (isFresh) mapBittensorSubnetPrices(response) else emptyMap()
}
