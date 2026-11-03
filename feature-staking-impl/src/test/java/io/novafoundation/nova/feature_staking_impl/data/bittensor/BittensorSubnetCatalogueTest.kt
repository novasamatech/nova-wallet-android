package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.di.modules.CommonModule
import io.novafoundation.nova.common.data.memory.ComputationalCache
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
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
import org.mockito.Mockito.verifyNoInteractions

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BittensorSubnetCatalogueTest {

    private val calls = CatalogueCalls()

    private val runtimeCalls = object : MultiChainRuntimeCallsApi {
        override suspend fun forChain(chainId: String): RuntimeCallsApi = calls

        override suspend fun isSupported(chainId: String, section: String, method: String) = true
    }

    private val config = BittensorConfigSource { fallbackBittensorConfig() }

    private val swaps = mock(BittensorSwapSource::class.java)

    private val backend = CatalogueApi()

    private val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }

    private val catalogueSource = BittensorSubnetCatalogueSource(backend, urls)

    private val commonModule = CommonModule()

    private val computationalCache = commonModule.provideComputationalCache()

    private val repository = createRepository()

    private fun createRepository(
        config: BittensorConfigSource = this.config,
        chainPatienceMillis: Long = 4_000L,
        nanoTime: () -> Long = System::nanoTime,
        catalogueSource: BittensorSubnetCatalogueSource = this.catalogueSource,
        computationalCache: ComputationalCache = this.computationalCache
    ): RealBittensorSubnetRepository {
        val source = BittensorSubnetSource(runtimeCalls, catalogueSource, computationalCache, chainPatienceMillis, nanoTime)
        return RealBittensorSubnetRepository(source, config, swaps)
    }

    @Test
    fun `first load failure reaches the observer through the shared source`() = runBlocking<Unit> {
        calls.failure = IOException("Node down")
        backend.failure = IOException("Backend down")

        val result = withTimeout(3_000) { runCatching { repository.observeSubnets().first() } }

        assertEquals("Node down", result.exceptionOrNull()?.message)
    }

    @Test
    fun `background catalogue errors retain the first rows and retry without busy polling`() = runTest {
        val catalogueSource = mock(BittensorSubnetCatalogueSource::class.java)
        val computations = UnsharedComputations()
        val repository = createRepository(catalogueSource = catalogueSource, computationalCache = computations)
        var subscriptions = 0
        val refreshes = flow {
            if (++subscriptions == 1) throw IOException("Offline")
            emit(backendCatalogue(name = "Recovered"))
            awaitCancellation()
        }
        `when`(catalogueSource.availableSubnets()).thenReturn(backendCatalogue())
        `when`(catalogueSource.observeSubnets()).thenReturn(refreshes)
        val rows = mutableListOf<List<io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet>>()
        val observer = backgroundScope.launch { repository.observeSubnets().toList(rows) }
        runCurrent()

        assertEquals(listOf("Backend 7"), rows.map { it.single().name })
        advanceTimeBy(29_999)
        runCurrent()
        assertEquals(1, subscriptions)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("Backend 7", "Recovered"), rows.map { it.single().name })
        assertEquals(0, calls.requests)

        observer.cancelAndJoin()
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(2, subscriptions)
    }

    @Test
    fun `cancellation of backend context resolution is never replaced by a runtime read`() = runBlocking<Unit> {
        val unavailableUrls = InfrastructureUrls { throw CancellationException("Cancelled") }
        val unavailableCatalogue = BittensorSubnetCatalogueSource(backend, unavailableUrls)
        val repository = createRepository(catalogueSource = unavailableCatalogue)

        assertTrue(runCatching { repository.catalogue() }.exceptionOrNull() is CancellationException)
        assertEquals(0, calls.requests)
        assertEquals(0, backend.requests)
    }

    @Test
    fun `saved catalogue is followed by refreshed metadata without a runtime request`() = runBlocking<Unit> {
        withTimeout(5_000) {
            var now = 0L
            val catalogueSource = BittensorSubnetCatalogueSource(backend, urls) { now }
            catalogueSource.subnets()
            now = 31.seconds.inWholeNanoseconds
            val response = CompletableDeferred<Unit>()
            backend.release = response
            backend.name = "Updated name"
            backend.cacheControl = "max-age=270"
            val repository = createRepository(catalogueSource = catalogueSource)
            val initial = CompletableDeferred<List<io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet>>()
            val updated = async {
                repository.observeSubnets()
                    .onEach { initial.complete(it) }
                    .first { it.single().name == "Updated name" }
            }

            assertEquals("Backend 7", initial.await().single().name)
            assertEquals(0, calls.requests)
            response.complete(Unit)

            assertEquals("Updated name", updated.await().single().name)
            assertEquals(2, backend.requests)
            assertEquals(0, calls.requests)
            verifyNoInteractions(swaps)
        }
    }

    @Test
    fun `failed backend context lookup does not prevent a healthy runtime catalogue`() = runBlocking<Unit> {
        val unavailableUrls = InfrastructureUrls { throw IOException("Infrastructure unavailable") }
        val unavailableCatalogue = BittensorSubnetCatalogueSource(backend, unavailableUrls)
        val repository = createRepository(catalogueSource = unavailableCatalogue)

        val catalogue = repository.catalogue()

        assertTrue(catalogue.fromChain)
        assertEquals(listOf("Name"), catalogue.subnets.map { it.name })
        assertEquals(1, calls.requests)
        assertEquals(0, backend.requests)
    }

    @Test
    fun `observing a healthy runtime catalogue does not fetch backend metadata`() = runBlocking<Unit> {
        val catalogue = withTimeout(3_000) { repository.observeSubnets().first() }

        assertEquals(listOf("Name"), catalogue.map { it.name })
        assertEquals(1, calls.requests)
        assertEquals(0, backend.requests)
    }

    @Test
    fun `cached portfolio metadata and picker receive the same late logo without any chain query`() = runBlocking<Unit> {
        withTimeout(5_000) {
            backend.cacheControl = "public, max-age=270"
            catalogueSource.subnets()
            val response = CompletableDeferred<BittensorConfigRemote>()
            var configRequests = 0
            val api = object : BittensorConfigApi {
                override suspend fun config(url: String): BittensorConfigRemote {
                    configRequests += 1
                    return response.await()
                }
            }
            val logger = mock(Logger::class.java)
            val config = RemoteBittensorConfigSource(api, "https://config.test/config.json", logger)
            val metadataRepository = createRepository(config = config)
            val browseRepository = BittensorSubnetBrowseRepository(catalogueSource, config)
            val firstMetadata = CompletableDeferred<List<io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet>>()
            val firstBrowse = CompletableDeferred<BittensorSubnetBrowseRepository.Catalogue>()
            val metadata = async {
                metadataRepository.observeSubnets().onEach { firstMetadata.complete(it) }.first { it.single().logoUrl != null }
            }
            val browse = async {
                browseRepository.observe().onEach { firstBrowse.complete(it) }.first { it.subnets.single().logoUrl != null }
            }

            assertNull(firstMetadata.await().single().logoUrl)
            assertNull(firstBrowse.await().subnets.single().logoUrl)
            assertEquals(0, calls.requests)
            response.complete(BittensorConfigRemote(null, listOf(BittensorConfigRemote.Subnet(7, "https://logos.test/7.png"))))

            assertEquals("https://logos.test/7.png", metadata.await().single().logoUrl)
            assertEquals(metadata.await().single().logoUrl, browse.await().subnets.single().logoUrl)
            assertEquals("https://logos.test/7.png", metadataRepository.subnets().single().logoUrl)
            assertEquals(1, configRequests)
            assertEquals(1, backend.requests)
            assertEquals(0, calls.requests)
        }
    }

    @Test
    fun `slow chain node falls back to backend data without altering reported prices`() = runBlocking<Unit> {
        calls.delayMillis = 5_000L
        val slowNodeRepository = createRepository(chainPatienceMillis = 50L)

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
    fun `persisted backend catalogue renders immediately on a later repository instance`() = runBlocking<Unit> {
        calls.failure = IOException("Node down")
        assertEquals(listOf("Backend 7"), repository.subnets().map { it.name })

        calls.failure = null
        val freshRepository = createRepository()
        assertEquals(listOf("Backend 7"), freshRepository.subnets().map { it.name })
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
        val slowBackendRepository = createRepository(chainPatienceMillis = 50L)

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
        val clockedRepository = createRepository(nanoTime = { now })
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
        val remoteConfig = RemoteBittensorConfigSource(api, "https://config.test/config.json", logger)
        val configlessRepository = createRepository(config = remoteConfig)

        val subnets = configlessRepository.observeSubnets().first()

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
        val source = BittensorSubnetCatalogueSource(backend, urls) { now }

        source.subnets()
        now = 29_000_000_000L
        source.subnets()
        assertEquals(1, backend.requests)
        now = 30_000_000_000L
        source.subnets()
        assertEquals(2, backend.requests)
    }

    @Test
    fun `rejected backend catalogue is fetched again despite its max-age`() = runBlocking<Unit> {
        backend.cacheControl = "max-age=30"
        backend.availability = "UNAVAILABLE"
        val source = BittensorSubnetCatalogueSource(backend, urls) { 0L }

        repeat(2) { assertTrue(runCatching { source.subnets() }.isFailure) }

        assertEquals(2, backend.requests)
    }

    private class CatalogueApi : BittensorSubnetCatalogueApi {

        var requests = 0

        var failure: Exception? = null

        var delayMillis = 0L

        var cacheControl = "no-store"

        var availability = "AVAILABLE"

        var name = "Backend 7"

        var release: CompletableDeferred<Unit>? = null

        override suspend fun subnets(url: String): Response<BittensorSubnetCatalogueRemote> {
            requests++
            delay(delayMillis)
            release?.await()
            failure?.let { throw it }
            return Response.success(backendCatalogue(availability = availability, name = name), Headers.headersOf("Cache-Control", cacheControl))
        }
    }

    private class UnsharedComputations : ComputationalCache {

        override suspend fun <T> useCache(key: String, scope: CoroutineScope, computation: suspend CoroutineScope.() -> T): T = computation(scope)

        override fun <T> useSharedFlow(key: String, scope: CoroutineScope, flowLazy: suspend CoroutineScope.() -> Flow<T>): Flow<T> = flow {
            emitAll(flowLazy(scope))
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

private fun backendCatalogue(freshness: String = "FRESH", availability: String = "AVAILABLE", name: String = "Backend 7"): BittensorSubnetCatalogueRemote {
    val row = BittensorSubnetCatalogueRemote.Subnet(7, "0.5", name, "B7", 42.toBigInteger(), "2000", alphaReserve = "4000")
    val component = BittensorRecommendationsRemote.Component(availability, freshness)
    val meta = BittensorSubnetCatalogueRemote.Meta(BittensorSubnetCatalogueRemote.Components(component, component))
    return BittensorSubnetCatalogueRemote(listOf(row), meta)
}
