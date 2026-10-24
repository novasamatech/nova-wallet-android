package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.call.MultiChainRuntimeCallsApi
import io.novafoundation.nova.runtime.call.RuntimeCallsApi
import io.novafoundation.nova.runtime.ext.ChainGeneses
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
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

    private val catalogueRepository = BittensorSubnetCatalogueRepository(backend, urls)

    private val repository = RealBittensorSubnetRepository(runtimeCalls, config, swaps, catalogueRepository)

    @Test
    fun `slow chain node falls back to backend names within the patience window`() = runBlocking<Unit> {
        calls.delayMillis = 5_000L
        val slowNodeRepository = RealBittensorSubnetRepository(runtimeCalls, config, swaps, catalogueRepository, chainPatienceMillis = 50L)

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
        val freshRepository = RealBittensorSubnetRepository(runtimeCalls, config, swaps, catalogueRepository)
        assertEquals(listOf("Name"), freshRepository.subnets().map { it.name })
    }

    @Test
    fun `stale backend catalogue is still listed but its price values nothing`() {
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
        val slowBackendRepository = RealBittensorSubnetRepository(runtimeCalls, config, swaps, catalogueRepository, chainPatienceMillis = 50L)

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

    @Test
    fun `last good catalogue keeps names without prices and never counts as a fresh load`() = runBlocking<Unit> {
        var now = 0L
        val clockedRepository = RealBittensorSubnetRepository(runtimeCalls, config, swaps, catalogueRepository, nanoTime = { now })
        clockedRepository.catalogue()
        calls.failure = IOException("Node down")
        backend.failure = IOException("Backend down")

        now = 31.seconds.inWholeNanoseconds
        val lastGood = clockedRepository.catalogue()
        now = 32.seconds.inWholeNanoseconds
        clockedRepository.catalogue()

        assertFalse(lastGood.fromChain)
        assertEquals(listOf("Name"), lastGood.subnets.map { it.name })
        assertNull(lastGood.subnets.single().priceInRao)
        assertNull(lastGood.subnets.single().taoReserveInPlanks)
        assertNull(lastGood.subnets.single().alphaReserveInPlanks)
        assertEquals(3, calls.requests)
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
        val configlessRepository = RealBittensorSubnetRepository(runtimeCalls, remoteConfig, swaps, catalogueRepository)

        val subnets = configlessRepository.subnets()

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

    @Test
    fun `backend catalogue is shared while it is HTTP-fresh`() = runBlocking<Unit> {
        var now = 0L
        backend.cacheControl = "max-age=30"
        val source = BittensorSubnetCatalogueRepository(backend, urls) { now }

        source.catalogue()
        now = 29_000_000_000L
        source.catalogue()
        assertEquals(1, backend.requests)
        now = 30_000_000_000L
        source.catalogue()
        assertEquals(2, backend.requests)
    }

    @Test
    fun `rejected backend catalogue is fetched again despite its max-age`() = runBlocking<Unit> {
        backend.cacheControl = "max-age=30"
        backend.availability = "UNAVAILABLE"
        val source = BittensorSubnetCatalogueRepository(backend, urls) { 0L }

        source.catalogue()
        source.catalogue()

        assertEquals(2, backend.requests)
    }

    private class CatalogueApi : BittensorSubnetCatalogueApi {

        var requests = 0

        var failure: Exception? = null

        var delayMillis = 0L

        var cacheControl = "no-store"

        var availability = "AVAILABLE"

        override suspend fun subnets(url: String): Response<BittensorSubnetCatalogueRemote> {
            requests++
            delay(delayMillis)
            failure?.let { throw it }
            return Response.success(backendCatalogue(availability = availability), Headers.headersOf("Cache-Control", cacheControl))
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
