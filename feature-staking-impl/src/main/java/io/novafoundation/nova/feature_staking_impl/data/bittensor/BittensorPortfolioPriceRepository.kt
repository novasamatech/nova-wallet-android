package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.hash.isPositive
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
        val backend = loadPrices("backend") { mapBittensorPortfolioPrices(catalogueRepository.catalogue()) }
            .filterKeys { it in heldSubnets }
        val missing = heldSubnets - backend.keys
        if (missing.isEmpty()) return backend

        val fallback = loadPrices("runtime") { runtimePrices.prices(chainId) }
            .filterKeys { it in missing }
        return backend + fallback
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
    val fresh = requireAvailableBittensorComponent(response.meta?.components?.alphaPrices, "Portfolio prices")
    return if (fresh) mapBittensorSubnetPrices(response) else emptyMap()
}

/** Catalogue prices regardless of freshness - for listing, never for valuing holdings. */
internal fun mapBittensorSubnetPrices(response: BittensorSubnetCatalogueRemote): Map<BittensorNetuid.Subnet, BigInteger> {
    val subnets = requireNotNull(response.items)
    val seenSubnets = mutableSetOf<BittensorNetuid.Subnet>()

    return buildMap {
        for (subnet in subnets) {
            val netuid = BittensorNetuid.from(requireNotNull(subnet.netuid))
            if (netuid !is BittensorNetuid.Subnet || !seenSubnets.add(netuid)) continue

            val reportedPrice = subnet.taoPerAlpha ?: continue
            val price = reportedPrice
                .toBigDecimal()
                .multiply(BittensorProtocolUnits.RAO_PER_TAO.toBigDecimal())
                .toBigIntegerExact()
            require(price.isNonNegative) { "Negative subnet price" }
            if (price.isPositive()) put(netuid, price)
        }
    }
}
