package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorProtocolUnits
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novasama.substrate_sdk_android.hash.isPositive
import java.math.BigInteger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

/**
 * The backend `/subnets` catalogue for every reader; a response is reused only while it is HTTP-fresh and only once it passed
 * validation. A rejected response still reaches its reader, which reports the failure, and the next read fetches again.
 */
class BittensorSubnetCatalogueRepository(
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
        mapBittensorBackendCatalogue(catalogue)
        mapBittensorSubnetPrices(catalogue)
    }.isSuccess
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
