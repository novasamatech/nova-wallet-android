package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

/**
 * The backend `/subnets` catalogue for every reader; a response is reused only while it is HTTP-fresh and only once it passed
 * validation. A rejected response still reaches its reader, which reports the failure, and the next read fetches again.
 */
class BittensorSubnetCatalogueRemoteSource(
    private val api: BittensorSubnetCatalogueApi,
    private val infrastructureUrls: InfrastructureUrls,
    private val nanoTime: () -> Long = System::nanoTime
) {

    private class Cached(val url: String, val catalogue: BittensorSubnetCatalogueRemote, val deadlineNanos: Long)

    private val mutex = Mutex()

    private var cached: Cached? = null

    suspend fun catalogue(): BittensorSubnetCatalogueRemote = mutex.withLock {
        val url = infrastructureUrls.bittensorBackendUrl("subnets")
        cached?.takeIf { it.url == url && nanoTime() < it.deadlineNanos }?.let { return@withLock it.catalogue }
        val startedAt = nanoTime()
        val response = api.subnets(url)
        if (!response.isSuccessful) throw HttpException(response)
        val catalogue = requireNotNull(response.body())
        if (isAdmissible(catalogue)) cached = Cached(url, catalogue, startedAt + bittensorCacheLifetimeNanos(response))
        catalogue
    }

    private fun isAdmissible(catalogue: BittensorSubnetCatalogueRemote): Boolean = runCatching {
        mapBittensorListedSubnets(catalogue)
        mapBittensorSubnetPrices(catalogue)
    }.isSuccess
}

/** Every non-root netuid the catalogue lists, as iOS builds SubtensorSubnetCatalogue; an unavailable component fails the read. */
internal fun mapBittensorListedSubnets(remote: BittensorSubnetCatalogueRemote): Set<BittensorNetuid.Subnet> {
    requireAvailableBittensorComponent(remote.meta?.components?.subnetMetadata, "Subnet metadata")
    requireAvailableBittensorComponent(remote.meta?.components?.alphaPrices, "Subnet prices")
    return requireNotNull(remote.items).mapNotNullTo(mutableSetOf()) { BittensorNetuid.from(requireNotNull(it.netuid)) as? BittensorNetuid.Subnet }
}
