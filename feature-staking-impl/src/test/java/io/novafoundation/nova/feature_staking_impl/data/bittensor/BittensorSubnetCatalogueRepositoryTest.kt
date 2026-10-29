package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class BittensorSubnetCatalogueRepositoryTest {

    private var now = 0L

    private var origin = "https://infra.test/"

    private val urls = InfrastructureUrls { origin.toHttpUrl() }

    private val api = CatalogueApi(catalogue())

    private val repository = BittensorSubnetCatalogueRepository(
        api,
        urls,
        nanoTime = { now }
    )

    @Test
    fun `malformed metadata and reserves reach the reader but cannot seed cache`() = runTest {
        val malformedRows = listOf(
            row(name = null),
            row(symbol = null),
            row(registration = null),
            row(registration = -BigInteger.ONE),
            row(taoReserve = null),
            row(taoReserve = "broken"),
            row(taoReserve = "-1"),
            row(alphaReserve = "-1"),
            row(price = "broken")
        )
        for (row in malformedRows) {
            val malformed = catalogue(listOf(row))
            api.body = malformed
            val previousRequests = api.requests.size

            assertSame(malformed, repository.catalogue())
            api.body = catalogue()
            assertSame(api.body, repository.catalogue())
            assertEquals(previousRequests + 2, api.requests.size)

            now += 30_000_000_000L
        }
    }

    @Test
    fun `bad metadata does not hide independently usable fresh prices`() = runTest {
        api.body = catalogue(listOf(row(name = null)))

        assertEquals(mapOf(BittensorNetuid.Subnet(1) to BigInteger.valueOf(500_000_000)), mapBittensorPortfolioPrices(repository.catalogue()))
        repository.catalogue()
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `missing TAO reserve leaves fresh prices readable and a corrected browse response retryable`() = runTest {
        api.body = catalogue(listOf(row(taoReserve = null)))

        val incomplete = repository.catalogue()
        val runtimeSubnets = mapBittensorBackendCatalogue(incomplete)
        val browseFailure = runCatching { mapBittensorBrowseSubnets(incomplete, runtimeSubnets) }.exceptionOrNull()

        assertTrue(browseFailure is IllegalArgumentException)
        assertEquals(
            mapOf(BittensorNetuid.Subnet(1) to BigInteger.valueOf(500_000_000)),
            mapBittensorPortfolioPrices(incomplete)
        )

        api.body = catalogue()
        val corrected = repository.catalogue()

        assertSame(api.body, corrected)
        assertEquals(1, mapBittensorBrowseSubnets(corrected, runtimeSubnets).size)
        assertSame(corrected, repository.catalogue())
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `stale but available valid catalogue can be reused without valuing holdings`() = runTest {
        api.body = catalogue(freshness = "STALE")

        val first = repository.catalogue()

        assertSame(first, repository.catalogue())
        assertTrue(mapBittensorPortfolioPrices(first).isEmpty())
        assertEquals(1, api.requests.size)
    }

    @Test
    fun `unavailable missing and empty catalogue bodies are not admitted`() = runTest {
        val rejectedBodies = listOf(
            catalogue(availability = "UNAVAILABLE"),
            BittensorSubnetCatalogueRemote(items = null, catalogue().meta),
            BittensorSubnetCatalogueRemote(listOf(row()), meta = null),
            catalogue(rows = emptyList())
        )
        for (body in rejectedBodies) {
            api.body = body
            val previousRequests = api.requests.size

            assertSame(body, repository.catalogue())
            assertSame(body, repository.catalogue())

            assertEquals(previousRequests + 2, api.requests.size)
        }
    }

    @Test
    fun `cache deadline subtracts HTTP age and starts before transport`() = runTest {
        api.headers = Headers.headersOf("Cache-Control", "max-age=30", "Age", "5")
        api.beforeResponse = { now += 10_000_000_000L }
        repository.catalogue()
        api.beforeResponse = {}

        now = 24_999_999_999L
        repository.catalogue()
        assertEquals(1, api.requests.size)
        now++
        repository.catalogue()
        assertEquals(2, api.requests.size)
    }

    @Test
    fun `origin changes do not reuse another origin catalogue`() = runTest {
        repository.catalogue()
        origin = "https://another.test/"
        repository.catalogue()

        assertEquals(listOf("https://infra.test/v1/bittensor/subnets", "https://another.test/v1/bittensor/subnets"), api.requests)
    }

    @Test
    fun `no store and no cache responses are fetched on every read`() = runTest {
        for (directive in listOf("no-store, max-age=30", "no-cache, max-age=30")) {
            api.headers = Headers.headersOf("Cache-Control", directive)
            val previousRequests = api.requests.size

            repository.catalogue()
            repository.catalogue()

            assertEquals(previousRequests + 2, api.requests.size)
        }
    }

    @Test
    fun `simultaneous readers share one validated response`() = runTest {
        api.beforeResponse = { delay(1L) }

        val first = async { repository.catalogue() }
        val second = async { repository.catalogue() }

        assertSame(first.await(), second.await())
        assertEquals(1, api.requests.size)
    }

    @Test
    fun `cancelled request releases the lock and cannot populate cache`() = runTest {
        api.beforeResponse = { awaitCancellation() }
        val request = async { repository.catalogue() }
        testScheduler.runCurrent()
        request.cancel()
        assertTrue(runCatching { request.await() }.exceptionOrNull() is CancellationException)

        api.beforeResponse = {}
        assertSame(api.body, repository.catalogue())
        assertEquals(2, api.requests.size)
    }

    private class CatalogueApi(var body: BittensorSubnetCatalogueRemote) : BittensorSubnetCatalogueApi {

        val requests = mutableListOf<String>()

        var headers = Headers.headersOf("Cache-Control", "max-age=30")

        var beforeResponse: suspend () -> Unit = {}

        override suspend fun subnets(url: String): Response<BittensorSubnetCatalogueRemote> {
            requests.add(url)
            beforeResponse()
            return Response.success(body, headers)
        }
    }

    private fun catalogue(
        rows: List<BittensorSubnetCatalogueRemote.Subnet> = listOf(row()),
        freshness: String = "FRESH",
        availability: String = "AVAILABLE"
    ): BittensorSubnetCatalogueRemote {
        val component = BittensorRecommendationsRemote.Component(availability, freshness)
        val components = BittensorSubnetCatalogueRemote.Components(component, component)
        val meta = BittensorSubnetCatalogueRemote.Meta(components)
        return BittensorSubnetCatalogueRemote(rows, meta)
    }

    private fun row(
        name: String? = "Subnet 1",
        symbol: String? = "S1",
        registration: BigInteger? = BigInteger.TEN,
        taoReserve: String? = "2000",
        alphaReserve: String? = "4000",
        price: String? = "0.5"
    ) = BittensorSubnetCatalogueRemote.Subnet(1, price, name, symbol, registration, taoReserve, alphaReserve)
}
