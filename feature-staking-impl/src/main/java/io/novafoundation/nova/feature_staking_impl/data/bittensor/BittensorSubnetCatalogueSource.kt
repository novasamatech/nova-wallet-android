package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow

class BittensorSubnetCatalogueSource(
    private val client: BittensorBackendClient,
    private val catalogueApi: BittensorSubnetCatalogueApi
) {

    suspend fun subnets(): BittensorSubnetCatalogueRemote = client.get(
        url = client.url("subnets"),
        type = BittensorSubnetCatalogueRemote::class.java,
        allowStale = true,
        validate = ::validateCatalogue,
        fetch = catalogueApi::subnets
    )

    /** Valuation must refresh an expired catalogue; display reads may still show its previous values. */
    suspend fun valuationPrices(): Map<BittensorNetuid.Subnet, BigInteger> {
        val catalogue = client.get(
            url = client.url("subnets"),
            type = BittensorSubnetCatalogueRemote::class.java,
            validate = ::validateCatalogue,
            fetch = catalogueApi::subnets
        )
        return mapBittensorPortfolioPrices(catalogue)
    }

    /** Returns immediately available data and lets the cache refresh it in the background when needed. */
    suspend fun availableSubnets(): BittensorSubnetCatalogueRemote? {
        client.peek(client.url("subnets"), BittensorSubnetCatalogueRemote::class.java, validate = ::validateCatalogue) ?: return null
        return subnets()
    }

    fun observeSubnets(): Flow<BittensorSubnetCatalogueRemote> = client.observe(
        url = { client.url("subnets") },
        type = BittensorSubnetCatalogueRemote::class.java,
        validate = ::validateCatalogue,
        fetch = catalogueApi::subnets
    )

    private fun validateCatalogue(catalogue: BittensorSubnetCatalogueRemote) {
        mapBittensorListedSubnets(catalogue)
        mapBittensorSubnetPrices(catalogue)
        mapBittensorBackendCatalogue(catalogue)
    }
}
