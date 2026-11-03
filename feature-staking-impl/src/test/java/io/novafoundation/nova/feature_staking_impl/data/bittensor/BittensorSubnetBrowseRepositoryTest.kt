package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.mockito.Mockito.mock
import retrofit2.Response

class BittensorSubnetBrowseRepositoryTest {

    @Test
    fun `manual catalogue requires runtime registration and price and backend membership`() {
        val runtime = listOf(subnet(1, 10, 1), subnet(2, 11, 1), subnet(3, 10, null), subnet(4, 10, 1))
        val rows = (1..3).map { id -> BittensorSubnetCatalogueRemote.Subnet(id, "1", "Backend $id", "S$id", 10.toBigInteger(), "2000") }
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH")
        val components = BittensorSubnetCatalogueRemote.Components(component, component)
        val meta = BittensorSubnetCatalogueRemote.Meta(components)
        val response = BittensorSubnetCatalogueRemote(rows, meta)

        val mapped = mapBittensorBrowseSubnets(response, runtime)

        assertEquals(listOf(1), mapped.map { it.id.value })
        assertEquals("Backend 1", mapped.single().name)
        assertEquals(2_000_000_000_000.toBigInteger(), mapped.single().taoReserveInPlanks)
    }

    @Test
    fun `unnamed subnet placeholder is mapped to a blank name so the screen shows Subnet netuid`() {
        val runtime = listOf(subnet(42, 10, 1))
        val rows = listOf(BittensorSubnetCatalogueRemote.Subnet(42, "1", "Unknown", "S42", 10.toBigInteger(), "2000"))
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH")
        val components = BittensorSubnetCatalogueRemote.Components(component, component)
        val response = BittensorSubnetCatalogueRemote(rows, BittensorSubnetCatalogueRemote.Meta(components))

        assertEquals("", mapBittensorBrowseSubnets(response, runtime).single().name)
        assertEquals("", normalizeBittensorSubnetName(" unknown "))
        assertEquals("Chutes", normalizeBittensorSubnetName("Chutes"))
    }

    @Test
    fun `stale backend metadata and prices are still listed but unavailable ones are not`() {
        val runtime = listOf(subnet(1, 10, 1))
        val rows = listOf(BittensorSubnetCatalogueRemote.Subnet(1, "1", "Backend 1", "S1", 10.toBigInteger(), "2000"))
        val stale = BittensorRecommendationsRemote.Component("AVAILABLE", "STALE")
        val unavailable = BittensorRecommendationsRemote.Component("UNAVAILABLE", "STALE")

        val listed = mapBittensorBrowseSubnets(
            BittensorSubnetCatalogueRemote(rows, BittensorSubnetCatalogueRemote.Meta(BittensorSubnetCatalogueRemote.Components(stale, stale))),
            runtime
        )
        val rejected = runCatching {
            mapBittensorBrowseSubnets(
                BittensorSubnetCatalogueRemote(rows, BittensorSubnetCatalogueRemote.Meta(BittensorSubnetCatalogueRemote.Components(stale, unavailable))),
                runtime
            )
        }

        assertEquals(listOf(1), listed.map { it.id.value })
        assertTrue(rejected.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `backend catalogue standing in for a slow node lists rows at the stale backend price`() {
        val fallback = listOf(subnet(1, 10, null))
        val rows = listOf(BittensorSubnetCatalogueRemote.Subnet(1, "1", "Backend 1", "S1", 10.toBigInteger(), "2000"))
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", "STALE")
        val components = BittensorSubnetCatalogueRemote.Components(component, component)
        val response = BittensorSubnetCatalogueRemote(rows, BittensorSubnetCatalogueRemote.Meta(components))

        val listed = mapBittensorBrowseSubnets(response, fallback, chainVerified = false)

        assertEquals(emptyList<BittensorSubnet>(), mapBittensorBrowseSubnets(response, fallback, chainVerified = true))
        assertEquals(listOf(1), listed.map { it.id.value })
        assertEquals(1_000_000_000.toBigInteger(), listed.single().priceInRao)
    }

    @Test
    fun `validator count is permit population independent of policy selection`() {
        val permits = List(20) { true } + List(8) { false }
        assertEquals(20, bindBittensorValidatorCount(permits))
        assertEquals(0, bindBittensorValidatorCount(emptyList<Boolean>()))
    }

    @Test
    fun `a new picker instance renders persisted catalogue without any node dependency`() = runBlocking<Unit> {
        val api = SwitchableCatalogueApi()
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val dao = InMemoryResponseCacheDao()
        val onlineCache = createBittensorTestSources(urls, dao = dao, catalogueApi = api)
        val onlineCatalogue = onlineCache.catalogue
        val config = BittensorConfigSource { fallbackBittensorConfig() }
        val online = BittensorSubnetBrowseRepository(onlineCatalogue, config)
        assertEquals("Backend", online.observe().first().subnets.single().name)

        api.failure = java.io.IOException("Offline")
        val restartedCache = createBittensorTestSources(urls, dao = dao, catalogueApi = api)
        val offlineCatalogue = restartedCache.catalogue
        val offline = BittensorSubnetBrowseRepository(offlineCatalogue, config)
        val snapshot = offline.observe().first()
        assertEquals("Backend", snapshot.subnets.single().name)
        assertTrue(!snapshot.stale)
    }

    @Test
    fun `logos arrive after the cached list without blocking its first render`() = runBlocking<Unit> {
        withTimeout(5_000) {
            val api = SwitchableCatalogueApi("public, max-age=270")
            val cache = createBittensorTestSources(catalogueApi = api)
            val catalogue = cache.catalogue
            catalogue.subnets()
            val logos = CompletableDeferred<BittensorConfig>()
            val config = BittensorConfigSource { logos.await() }
            val repository = BittensorSubnetBrowseRepository(catalogue, config)
            val initial = CompletableDeferred<BittensorSubnetBrowseRepository.Catalogue>()
            val decorated = async {
                repository.observe().onEach { initial.complete(it) }
                    .first { it.subnets.single().logoUrl != null }
            }

            assertEquals("Backend", initial.await().subnets.single().name)
            assertEquals(null, initial.await().subnets.single().logoUrl)
            assertTrue(!decorated.isCompleted)
            val subnetId = BittensorNetuid.Subnet(1)
            logos.complete(fallbackBittensorConfig().copy(subnetLogos = mapOf(subnetId to "https://logos.test/1.png")))

            assertEquals("https://logos.test/1.png", decorated.await().subnets.single().logoUrl)
            assertEquals(1, api.requests)
        }
    }

    @Test
    fun `reopening browse restores logos from the shared config without refetching`() = runBlocking<Unit> {
        val api = SwitchableCatalogueApi("public, max-age=270")
        val cache = createBittensorTestSources(catalogueApi = api)
        val catalogue = cache.catalogue
        var configRequests = 0
        val configApi = object : BittensorConfigApi {
            override suspend fun config(url: String): BittensorConfigRemote {
                configRequests += 1
                return BittensorConfigRemote(null, listOf(BittensorConfigRemote.Subnet(1, "https://logos.test/1.png")))
            }
        }
        val logger = mock(Logger::class.java)
        val config = RemoteBittensorConfigSource(configApi, "https://config.test/config.json", logger)
        val first = BittensorSubnetBrowseRepository(catalogue, config)
        val second = BittensorSubnetBrowseRepository(catalogue, config)

        withTimeout(5_000) {
            assertEquals("https://logos.test/1.png", first.observe().first { it.subnets.single().logoUrl != null }.subnets.single().logoUrl)
            assertEquals("https://logos.test/1.png", second.observe().first { it.subnets.single().logoUrl != null }.subnets.single().logoUrl)
        }
        assertEquals(1, configRequests)
        assertEquals(1, api.requests)
    }

    @Test
    fun `reopening browse within max age reuses one backend request`() = runBlocking<Unit> {
        val api = SwitchableCatalogueApi("public, max-age=270")
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val cache = createBittensorTestSources(urls, catalogueApi = api)
        val catalogue = cache.catalogue
        val config = BittensorConfigSource { fallbackBittensorConfig() }
        val first = BittensorSubnetBrowseRepository(catalogue, config)
        val second = BittensorSubnetBrowseRepository(catalogue, config)
        first.observe().first()
        second.observe().first()
        assertEquals(1, api.requests)
    }

    private class SwitchableCatalogueApi(private val cacheControl: String = "no-store") : BittensorSubnetCatalogueApi {

        var failure: Exception? = null

        var requests = 0

        override suspend fun subnets(url: String): Response<BittensorSubnetCatalogueRemote> {
            requests += 1
            failure?.let { throw it }
            val row = BittensorSubnetCatalogueRemote.Subnet(1, "1", "Backend", "S", 10.toBigInteger(), "2000")
            val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH")
            val meta = BittensorSubnetCatalogueRemote.Meta(BittensorSubnetCatalogueRemote.Components(component, component))
            return Response.success(BittensorSubnetCatalogueRemote(listOf(row), meta), okhttp3.Headers.headersOf("Cache-Control", cacheControl))
        }
    }

    private fun subnet(id: Int, registration: Long, price: Long?) = BittensorSubnet(
        BittensorNetuid.Subnet(id),
        "Runtime $id",
        "S$id",
        null,
        priceInRao = price?.toBigInteger(),
        registrationBlock = registration.toBigInteger()
    )
}
