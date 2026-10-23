package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException

/** Manual buy catalogue only; held positions must never be filtered through this repository. */
class BittensorSubnetBrowseRepository(
    private val api: BittensorSubnetCatalogueApi,
    private val urls: InfrastructureUrls,
    private val subnets: BittensorSubnetRepository,
    private val availability: BittensorSubnetTradingAvailabilityRepository
) {

    suspend fun subnets(): List<BittensorSubnet> = coroutineScope {
        val runtime = async { subnets.catalogue() }
        val response = loadCatalogue()
        val catalogue = runtime.await()
        val candidates = mapBittensorBrowseSubnets(response, catalogue.subnets, chainVerified = catalogue.fromChain)
        val enabled = availability.enabledSubnets(candidates.map { it.id })
        candidates.filter { it.id in enabled }
    }

    private suspend fun loadCatalogue(): BittensorSubnetCatalogueRemote {
        val base = requireNotNull(urls.baseUrl())
        require(base.isHttps)
        val url = requireNotNull(base.resolve("/v1/bittensor/subnets"))
        val response = api.subnets(url.toString())
        if (!response.isSuccessful) throw HttpException(response)
        return requireNotNull(response.body())
    }
}

/**
 * With a chain catalogue every backend row is cross-checked against the chain (registration, live positive price).
 * When the backend catalogue stood in for a slow node ([chainVerified] = false) there is no chain view to compare
 * with: rows are listed on the backend's word, and subtoken availability is still read from chain storage.
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
        chain.copy(name = normalizeBittensorSubnetName(requireNotNull(row.name)), symbol = requireNotNull(row.symbol), taoReserveInPlanks = reserve)
    }
}
