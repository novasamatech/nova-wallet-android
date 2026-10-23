package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger
import java.io.IOException
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import retrofit2.Response
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class BittensorSubnetCatalogueTest {

    private val calls = CatalogueCalls()

    private val runtimeCalls = object : MultiChainRuntimeCallsApi {
        override suspend fun forChain(chainId: String): RuntimeCallsApi = calls

        override suspend fun isSupported(chainId: String, section: String, method: String) = true
    }

    private val config = BittensorConfigRepository { fallbackBittensorConfig() }

    private val swaps = mock(BittensorSwapSource::class.java)

    private val backend = CatalogueApi()

    private val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }

    private val repository = RealBittensorSubnetRepository(runtimeCalls, config, swaps, backend, urls)

    @Test
    fun `slow chain node falls back to backend names within the patience window`() = runBlocking<Unit> {
        calls.delayMillis = 5_000L
        val slowNodeRepository = RealBittensorSubnetRepository(runtimeCalls, config, swaps, backend, urls, chainPatienceMillis = 50L)

        val catalogue = slowNodeRepository.catalogue()
        val subnets = catalogue.subnets

        assertFalse(catalogue.fromChain)
        assertEquals(listOf("Backend 7"), subnets.map { it.name })
        assertEquals("B7", subnets.single().symbol)
        assertEquals(BigInteger.valueOf(500_000_000), subnets.single().priceInRao)
        assertEquals(BigInteger.valueOf(2_000_000_000_000), subnets.single().taoReserveInPlanks)
        assertEquals(BigInteger.valueOf(42), subnets.single().registrationBlock)
        assertEquals(1, backend.requests)
    }

    @Test
    fun `failing chain node uses backend and fast chain stays authoritative`() = runBlocking<Unit> {
        calls.failure = IOException("Node down")
        assertEquals(listOf("Backend 7"), repository.subnets().map { it.name })

        calls.failure = null
        val freshRepository = RealBittensorSubnetRepository(runtimeCalls, config, swaps, backend, urls)
        assertEquals(listOf("Name"), freshRepository.subnets().map { it.name })
    }

    @Test
    fun `stale backend catalogue is still listed but its price is not shown`() {
        val subnets = mapBittensorBackendCatalogue(backendCatalogue(freshness = "STALE"))

        assertEquals(listOf(BittensorNetuid.Subnet(7)), subnets.map { it.id })
        assertNull(subnets.single().priceInRao)
    }

    @Test
    fun `fast chain never spends an attested backend request`() = runBlocking<Unit> {
        val catalogue = repository.catalogue()

        assertTrue(catalogue.fromChain)
        assertEquals(0, backend.requests)
    }

    @Test
    fun `after the patience window the first source to answer wins`() = runBlocking<Unit> {
        calls.delayMillis = 300L
        backend.delayMillis = 5_000L
        val slowBackendRepository = RealBittensorSubnetRepository(runtimeCalls, config, swaps, backend, urls, chainPatienceMillis = 50L)

        val catalogue = slowBackendRepository.catalogue()

        assertTrue(catalogue.fromChain)
        assertEquals(listOf("Name"), catalogue.subnets.map { it.name })
        assertEquals(1, backend.requests)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unavailable backend catalogue is rejected`() {
        mapBittensorBackendCatalogue(backendCatalogue(availability = "UNAVAILABLE"))
    }

    @Test
    fun `both sources failing surface the chain failure`() = runBlocking<Unit> {
        calls.failure = IOException("Node down")
        backend.failure = IOException("Backend down")

        assertEquals("Node down", runCatching { repository.subnets() }.exceptionOrNull()?.message)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `quick pick rejects missing runtime rows`() {
        bindBittensorSubnets(listOf(null), requireComplete = true)
    }

    @Test
    fun `quick pick bypasses the browsing catalogue cache`() = runBlocking<Unit> {
        repository.subnets()
        repository.quickPickSubnets()
        repository.quickPickSubnets()

        assertEquals(3, calls.requests)
    }

    @Test
    fun `optional config failure does not block the runtime catalogue`() = runBlocking<Unit> {
        val api = object : BittensorConfigApi {
            override suspend fun config(url: String): BittensorConfigRemote = throw IOException("Config unavailable")
        }
        val logger = mock(Logger::class.java)
        val remoteConfig = RemoteBittensorConfigRepository(api, "https://config.test/config.json", logger)
        val catalogueRepository = RealBittensorSubnetRepository(runtimeCalls, remoteConfig, swaps, backend, urls)

        val subnets = catalogueRepository.subnets()

        assertEquals(listOf(BittensorNetuid.Subnet(1)), subnets.map { it.id })
        assertEquals(1, calls.requests)
    }

    @Test
    fun `reuses catalogue metadata but fetches a current price for individual subnet lookups`() = runBlocking<Unit> {
        val subnetId = BittensorNetuid.Subnet(1)
        `when`(swaps.currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)).thenReturn(BigInteger.TEN, BigInteger.valueOf(20))

        repository.subnets()
        repository.subnets()
        assertEquals(BigInteger.TEN, repository.subnet(subnetId).priceInRao)
        assertEquals(BigInteger.valueOf(20), repository.subnet(subnetId).priceInRao)

        assertEquals(1, calls.requests)
        verify(swaps, times(2)).currentAlphaPrice(ChainGeneses.BITTENSOR, subnetId)
    }

    private class CatalogueApi : BittensorSubnetCatalogueApi {

        var requests = 0

        var failure: Exception? = null

        var delayMillis = 0L

        override suspend fun subnets(url: String): Response<BittensorSubnetCatalogueRemote> {
            requests++
            delay(delayMillis)
            failure?.let { throw it }
            return Response.success(backendCatalogue())
        }
    }

    private class CatalogueCalls : RuntimeCallsApi {

        override val chainId = ChainGeneses.BITTENSOR

        override val runtime = mock(RuntimeSnapshot::class.java)

        var requests = 0

        var delayMillis = 0L

        var failure: Exception? = null

        override fun isSupported(section: String, method: String) = true

        override suspend fun <R> callAt(
            blockHash: String,
            section: String,
            method: String,
            arguments: Map<String, Any?>,
            returnBinding: (Any?) -> R
        ): R = error("Unexpected block-pinned request")

        override suspend fun <R> call(
            section: String,
            method: String,
            arguments: Map<String, Any?>,
            returnBinding: (Any?) -> R
        ): R {
            requests++
            delay(delayMillis)
            failure?.let { throw it }
            val subnet = Struct.Instance(
                mapOf(
                    "netuid" to BigInteger.ONE,
                    "subnetName" to "Name".encodeToByteArray(),
                    "tokenSymbol" to "α".encodeToByteArray(),
                    "taoIn" to BigInteger.valueOf(1000),
                    "alphaIn" to BigInteger.valueOf(1000)
                )
            )
            return returnBinding(listOf(subnet))
        }

        override suspend fun <R> call(
            section: String,
            method: String,
            arguments: List<Pair<Any?, String?>>,
            returnType: String,
            returnBinding: (Any?) -> R
        ): R = error("Unexpected untyped runtime request")
    }
}

private fun backendCatalogue(freshness: String = "FRESH", availability: String = "AVAILABLE"): BittensorSubnetCatalogueRemote {
    val row = BittensorSubnetCatalogueRemote.Subnet(7, "0.5", "Backend 7", "B7", 42.toBigInteger(), "2000", alphaReserve = "4000")
    val component = BittensorRecommendationsRemote.Component(availability, freshness)
    val meta = BittensorSubnetCatalogueRemote.Meta(BittensorSubnetCatalogueRemote.Components(component, component))
    return BittensorSubnetCatalogueRemote(listOf(row), meta)
}
