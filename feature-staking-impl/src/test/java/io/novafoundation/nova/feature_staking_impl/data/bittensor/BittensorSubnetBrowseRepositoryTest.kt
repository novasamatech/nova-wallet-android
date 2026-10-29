package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import io.novafoundation.nova.runtime.storage.source.query.StorageQueryContext
import java.io.IOException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
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
        val catalogueRepository = BittensorSubnetCatalogueRepository(api, urls)
        val logger = RecordingLogger()
        val repository = BittensorSubnetBrowseRepository(catalogueRepository, runtime, availability, logger)
        val row = BittensorSubnetCatalogueRemote.Subnet(1, "1", "Backend", "S", 10.toBigInteger(), "2000")
        val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH")
        val components = BittensorSubnetCatalogueRemote.Components(component, component)
        val meta = BittensorSubnetCatalogueRemote.Meta(components)
        val response = BittensorSubnetCatalogueRemote(listOf(row), meta)
        `when`(api.subnets("https://infra.test/v1/bittensor/subnets")).thenReturn(Response.success(response))
        `when`(runtime.catalogue()).thenReturn(BittensorSubnetCatalogue(listOf(subnet(1, 10, 1)), fromChain = true))

        assertEquals(emptyList<BittensorSubnet>(), repository.subnets())
    }

    @Test
    fun `reopening the picker offline serves the catalogue loaded before`() = runBlocking<Unit> {
        val api = SwitchableCatalogueApi()
        val storage = SwitchableStorage(mock(StorageDataSource::class.java))
        val logger = RecordingLogger()
        val repository = browseRepository(api, storage, logger = logger)
        val online = repository.subnets()
        val failure = IOException("Unable to resolve host")

        api.failure = failure
        storage.offline = true
        val offline = withTimeout(5_000L) { repository.subnets() }

        assertEquals(listOf(1), online.map { it.id.value })
        assertEquals(online, offline)
        // Coroutine stack-trace recovery may log a copy of the failure, so compare what it says.
        assertEquals(listOf(failure.message), logger.failures.map { it.message })
        assertTrue(logger.failures.single() is IOException)
    }

    @Test
    fun `a chain read that waits for a reconnecting node serves the catalogue loaded before`() = runBlocking<Unit> {
        val storage = SwitchableStorage(mock(StorageDataSource::class.java))
        val repository = browseRepository(SwitchableCatalogueApi(), storage, timeoutMillis = 100L)
        val online = repository.subnets()

        storage.offline = true

        assertEquals(online, withTimeout(5_000L) { repository.subnets() })
    }

    @Test
    fun `without a loaded catalogue an offline read fails instead of waiting`() = runBlocking<Unit> {
        val api = SwitchableCatalogueApi()
        val storage = SwitchableStorage(mock(StorageDataSource::class.java)).apply { offline = true }
        val repository = browseRepository(api, storage, timeoutMillis = 100L)

        val slowNode = runCatching { withTimeout(5_000L) { repository.subnets() } }.exceptionOrNull()
        api.failure = IOException("Unable to resolve host")
        val noNetwork = runCatching { withTimeout(5_000L) { repository.subnets() } }.exceptionOrNull()

        assertTrue(slowNode is BittensorTimeoutException)
        assertTrue(noNetwork is IOException)
    }

    @Test
    fun `an inner timeout without a loaded catalogue is reported as a slow read, not a cancellation`() = runBlocking<Unit> {
        val storage = SwitchableStorage(mock(StorageDataSource::class.java)).apply { innerTimeout = true }
        val repository = browseRepository(SwitchableCatalogueApi(), storage)

        val failure = runCatching { repository.subnets() }.exceptionOrNull()

        assertTrue(failure is BittensorTimeoutException)
        assertTrue(failure?.cause is TimeoutCancellationException)
    }

    @Test
    fun `caller timeout and cancellation cannot return the last loaded catalogue`() = runTest {
        val api = SwitchableCatalogueApi()
        val storageDelegate = mock(StorageDataSource::class.java)
        val storage = SwitchableStorage(storageDelegate)
        val repository = browseRepository(api, storage)
        repository.subnets()
        storage.offline = true

        val timeout = runCatching { withTimeout(1L) { repository.subnets() } }.exceptionOrNull()
        assertTrue(timeout is TimeoutCancellationException)

        storage.offline = false
        api.failure = CancellationException("Cancelled")
        assertTrue(runCatching { repository.subnets() }.exceptionOrNull() is CancellationException)
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

    private fun browseRepository(
        api: BittensorSubnetCatalogueApi,
        storage: StorageDataSource,
        timeoutMillis: Long = BITTENSOR_REQUEST_TIMEOUT,
        logger: Logger = RecordingLogger()
    ): BittensorSubnetBrowseRepository {
        val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }
        val runtime = mock(BittensorSubnetRepository::class.java)
        runBlocking { `when`(runtime.catalogue()).thenReturn(BittensorSubnetCatalogue(listOf(subnet(1, 10, 1)), fromChain = true)) }
        val availability = BittensorSubnetTradingAvailabilityRepository(storage)
        val catalogueRepository = BittensorSubnetCatalogueRepository(api, urls)
        return BittensorSubnetBrowseRepository(catalogueRepository, runtime, availability, logger, timeoutMillis)
    }

    private class RecordingLogger : Logger {

        val failures = mutableListOf<Throwable>()

        override fun log(message: String?) = Unit

        override fun log(throwable: Throwable?) {
            throwable?.let(failures::add)
        }
    }

    private class SwitchableCatalogueApi : BittensorSubnetCatalogueApi {

        var failure: Exception? = null

        override suspend fun subnets(url: String): Response<BittensorSubnetCatalogueRemote> {
            failure?.let { throw it }
            val row = BittensorSubnetCatalogueRemote.Subnet(1, "1", "Backend", "S", 10.toBigInteger(), "2000")
            val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH")
            val meta = BittensorSubnetCatalogueRemote.Meta(BittensorSubnetCatalogueRemote.Components(component, component))
            return Response.success(BittensorSubnetCatalogueRemote(listOf(row), meta))
        }
    }

    /** Every subnet is enabled; offline, the read waits for the socket like a reconnecting node. */
    private class SwitchableStorage(delegate: StorageDataSource) : StorageDataSource by delegate {

        var offline = false

        var innerTimeout = false

        @Suppress("UNCHECKED_CAST")
        override suspend fun <R> query(
            chainId: String,
            at: String?,
            applyStorageDefault: Boolean,
            query: suspend StorageQueryContext.() -> R
        ): R {
            if (offline) awaitCancellation()
            if (innerTimeout) withTimeout(1L) { awaitCancellation() }
            return mapOf(BittensorNetuid.Subnet(1) to true) as R
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
