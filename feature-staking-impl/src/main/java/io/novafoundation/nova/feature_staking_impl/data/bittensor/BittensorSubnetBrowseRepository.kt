package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import io.novasama.substrate_sdk_android.hash.isPositive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Browsing names is independent of node connectivity. Eligibility is checked when selecting and before signing. */
class BittensorSubnetBrowseRepository(
    private val catalogueSource: BittensorSubnetCatalogueSource,
    private val configSource: BittensorConfigSource
) {

    data class Catalogue(val subnets: List<BittensorSubnet>, val stale: Boolean)

    // Like iOS, logos decorate an already visible list and never delay the catalogue.
    fun observe(): Flow<Catalogue> = combine(
        catalogueSource.observeSubnets(),
        configSource.observeSubnetLogos()
    ) { snapshot, logos ->
        val components = snapshot.meta?.components
        val backendStale = components?.subnetMetadata?.freshness != "FRESH" || components.alphaPrices?.freshness != "FRESH"
        val subnets = mapCatalogue(snapshot).map { subnet -> subnet.copy(logoUrl = logos[subnet.id]) }
        Catalogue(subnets, stale = backendStale)
    }

    private fun mapCatalogue(remote: BittensorSubnetCatalogueRemote): List<BittensorSubnet> {
        val metadata = mapBittensorBackendCatalogue(remote)
        return mapBittensorBrowseSubnets(remote, metadata, chainVerified = false)
    }
}

/**
 * With a chain catalogue every backend row is cross-checked against the chain (registration, live positive price).
 * When the chain could not supply one ([chainVerified] = false) there is no chain view to compare with: rows are
 * listed with the backend price. Live trading availability is checked by transaction preflight, not by browsing.
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
