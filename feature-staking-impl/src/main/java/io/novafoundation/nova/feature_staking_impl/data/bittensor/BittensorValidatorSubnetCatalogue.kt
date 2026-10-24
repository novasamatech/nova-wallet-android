package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import java.math.BigInteger
import retrofit2.HttpException

/** The backend `/subnets` catalogue as the validator info screen reads it: every listed row, with no chain or SubtokenEnabled filter. */
class BittensorValidatorSubnetCatalogue(
    private val api: BittensorSubnetCatalogueApi,
    private val urls: InfrastructureUrls
) {

    suspend fun subnets(): Set<BittensorNetuid.Subnet> = mapBittensorListedSubnets(load())

    /** The FRESH backend price of [subnet] in rao per alpha; null while the prices are STALE or the subnet is not listed. */
    suspend fun freshPrice(subnet: BittensorNetuid.Subnet): BigInteger? = mapBittensorPortfolioPrices(load())[subnet]

    private suspend fun load(): BittensorSubnetCatalogueRemote {
        val base = urls.baseUrl() ?: throw InfrastructureUrlMissingException()
        require(base.isHttps) { "Infrastructure requires HTTPS" }
        val url = requireNotNull(base.resolve("/v1/bittensor/subnets"))
        val response = api.subnets(url.toString())
        if (!response.isSuccessful) throw HttpException(response)
        return requireNotNull(response.body())
    }
}

/** Every non-root netuid the catalogue lists, as iOS builds SubtensorSubnetCatalogue; an unavailable component fails the read. */
internal fun mapBittensorListedSubnets(remote: BittensorSubnetCatalogueRemote): Set<BittensorNetuid.Subnet> {
    requireAvailableBittensorComponent(remote.meta?.components?.subnetMetadata, "Subnet metadata")
    requireAvailableBittensorComponent(remote.meta?.components?.alphaPrices, "Subnet prices")
    return requireNotNull(remote.items).mapNotNullTo(mutableSetOf()) { BittensorNetuid.from(requireNotNull(it.netuid)) as? BittensorNetuid.Subnet }
}
