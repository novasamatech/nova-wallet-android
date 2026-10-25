package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
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
        val response = BittensorSubnetCatalogueRemote(rows, BittensorSubnetCatalogueRemote.Meta(BittensorSubnetCatalogueRemote.Components(component, component)))

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
        val response = BittensorSubnetCatalogueRemote(rows, BittensorSubnetCatalogueRemote.Meta(BittensorSubnetCatalogueRemote.Components(component, component)))

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
    fun `manual buy excludes subtoken disabled entries`() = runBlocking<Unit> {
        val api = mock(BittensorSubnetCatalogueApi::class.java)
        val runtime = mock(BittensorSubnetRepository::class.java)
        val storageDelegate = mock(StorageDataSource::class.java)
        val storage = DisabledStorage(storageDelegate)
        val availability = BittensorSubnetTradingAvailabilityRepository(storage)
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val repository = BittensorSubnetBrowseRepository(BittensorSubnetCatalogueRepository(api, urls), runtime, availability)
        val row = BittensorSubnetCatalogueRemote.Subnet(1, "1", "Backend", "S", 10.toBigInteger(), "2000")
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH")
        val components = BittensorSubnetCatalogueRemote.Components(component, component)
        val meta = BittensorSubnetCatalogueRemote.Meta(components)
        val response = BittensorSubnetCatalogueRemote(listOf(row), meta)
        `when`(api.subnets("https://infra.test/v1/bittensor/subnets")).thenReturn(Response.success(response))
        `when`(runtime.catalogue()).thenReturn(BittensorSubnetCatalogue(listOf(subnet(1, 10, 1)), fromChain = true))

        assertEquals(emptyList<BittensorSubnet>(), repository.subnets())
    }

    private class DisabledStorage(delegate: StorageDataSource) : StorageDataSource by delegate {

        @Suppress("UNCHECKED_CAST")
        override suspend fun <R> query(
            chainId: String,
            at: String?,
            applyStorageDefault: Boolean,
            query: suspend StorageQueryContext.() -> R
        ): R = emptyMap<BittensorNetuid.Subnet, Boolean>() as R
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
