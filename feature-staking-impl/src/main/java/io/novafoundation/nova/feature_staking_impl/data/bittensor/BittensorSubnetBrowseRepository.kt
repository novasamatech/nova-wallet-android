package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novasama.substrate_sdk_android.hash.isPositive
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Manual buy catalogue only; held positions must never be filtered through this repository.
 *
 * Feature-scoped so the picker can be reopened offline: a failed or slow read serves the catalogue loaded last, and only
 * without one does the failure reach the screen. The whole read is bounded, since the chain availability read waits
 * for a reconnecting node indefinitely.
 */
class BittensorSubnetBrowseRepository(
    private val catalogueRepository: BittensorSubnetCatalogueRepository,
    private val subnets: BittensorSubnetRepository,
    private val availability: BittensorSubnetTradingAvailabilityRepository,
    private val logger: Logger,
    private val timeoutMillis: Long = BITTENSOR_REQUEST_TIMEOUT
) {

    @Volatile
    private var lastLoaded: List<BittensorSubnet>? = null

    suspend fun subnets(): List<BittensorSubnet> {
        val loaded = try {
            withTimeoutOrNull(timeoutMillis) { load() } ?: throw BittensorTimeoutException("Subnet browse catalogue")
        } catch (e: Exception) {
            // A child request timing out is a failed read; cancellation of the caller is not.
            currentCoroutineContext().ensureActive()
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            val cached = lastLoaded ?: throw if (e is TimeoutCancellationException) BittensorTimeoutException("Subnet catalogue", e) else e
            // Not silent: an online rejection or malformed answer must stay visible in the logs behind the cached list.
            logger.log("[Bittensor] Subnet browse catalogue read failed; serving the catalogue loaded last")
            logger.log(e)
            return cached
        }
        lastLoaded = loaded
        return loaded
    }

    private suspend fun load(): List<BittensorSubnet> = coroutineScope {
        val runtime = async { subnets.catalogue() }
        val response = catalogueRepository.catalogue()
        val catalogue = runtime.await()
        val candidates = mapBittensorBrowseSubnets(response, catalogue.subnets, chainVerified = catalogue.fromChain)
        val enabled = availability.enabledSubnets(candidates.map { it.id })
        candidates.filter { it.id in enabled }
    }
}

/**
 * With a chain catalogue every backend row is cross-checked against the chain (registration, live positive price).
 * When the chain could not supply one ([chainVerified] = false) there is no chain view to compare with: rows are
 * listed on the backend's word with the backend price, and subtoken availability is still read from chain storage.
 */
internal fun mapBittensorBrowseSubnets(
    remote: BittensorSubnetCatalogueRemote,
    runtime: List<BittensorSubnet>,
    chainVerified: Boolean = true
): List<BittensorSubnet> {
    // STALE metadata or prices are still listed, as on iOS; quotes and valuation read live chain prices.
    requireAvailableBittensorComponent(remote.meta?.components?.subnetMetadata, "Subnet metadata")
    requireAvailableBittensorComponent(remote.meta?.components?.alphaPrices, "Subnet prices")
    val prices = mapBittensorSubnetPrices(remote)
    val chainSubnets = runtime.associateBy { it.id }
    val seen = mutableSetOf<BittensorNetuid.Subnet>()
    return requireNotNull(remote.items).mapNotNull { row ->
        val id = BittensorNetuid.from(requireNotNull(row.netuid)) as? BittensorNetuid.Subnet ?: return@mapNotNull null
        if (!seen.add(id)) return@mapNotNull null
        val chain = chainSubnets[id] ?: return@mapNotNull null
        val registration = requireNotNull(row.networkRegisteredAt)
        require(registration.isNonNegative)
        val chainPriced = !chainVerified || chain.priceInRao?.isPositive() == true
        if (registration != chain.registrationBlock || !chainPriced || id !in prices) return@mapNotNull null
        val reserve = requireNotNull(row.taoReserve).toBigDecimal().movePointRight(9).toBigIntegerExact()
        require(reserve.isNonNegative)
        chain.copy(
            name = normalizeBittensorSubnetName(requireNotNull(row.name)),
            symbol = requireNotNull(row.symbol),
            priceInRao = if (chainVerified) chain.priceInRao else prices[id],
            taoReserveInPlanks = reserve
        )
    }
}
