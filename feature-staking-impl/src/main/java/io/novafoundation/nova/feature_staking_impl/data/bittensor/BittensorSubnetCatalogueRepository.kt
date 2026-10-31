package io.novafoundation.nova.feature_staking_impl.data.bittensor

import kotlinx.coroutines.flow.Flow

/** One persistent catalogue shared by discovery, pricing and validator coverage. */
class BittensorSubnetCatalogueRepository(
    private val api: BittensorSubnetCatalogueApi,
    private val cache: BittensorResponseCache
) {

    suspend fun catalogue(): BittensorSubnetCatalogueRemote = cache.get(
        path = "subnets",
        type = BittensorSubnetCatalogueRemote::class.java,
        allowStale = true,
        validate = ::validate,
        fetch = api::subnets
    ).forDisplay()

    suspend fun cached(): BittensorCachedResponse<BittensorSubnetCatalogueRemote>? =
        cache.peek("subnets", BittensorSubnetCatalogueRemote::class.java, validate = ::validate)

    fun observe(): Flow<BittensorCachedResponse<BittensorSubnetCatalogueRemote>> = cache.observe(
        path = "subnets",
        type = BittensorSubnetCatalogueRemote::class.java,
        validate = ::validate,
        fetch = api::subnets
    )

    private fun validate(catalogue: BittensorSubnetCatalogueRemote) {
        mapBittensorListedSubnets(catalogue)
        mapBittensorSubnetPrices(catalogue)
        mapBittensorBackendCatalogue(catalogue)
    }
}

private fun BittensorCachedResponse<BittensorSubnetCatalogueRemote>.forDisplay(): BittensorSubnetCatalogueRemote {
    if (!stale) return value
    val components = value.meta?.components
    val staleComponents = BittensorSubnetCatalogueRemote.Components(components?.alphaPrices?.asStale(), components?.subnetMetadata?.asStale())
    return BittensorSubnetCatalogueRemote(value.items, BittensorSubnetCatalogueRemote.Meta(staleComponents))
}

internal fun BittensorRecommendationsRemote.Component.asStale() =
    BittensorRecommendationsRemote.Component(availability, "STALE", asOf, availabilityReason)
