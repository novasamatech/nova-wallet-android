package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import retrofit2.Response

class BittensorPortfolioPriceRepositoryTest {

    private val runtime = mock(BittensorRuntimePriceSource::class.java)

    private val logger = mock(Logger::class.java)

    private val urls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }

    private var backendResponse = response("0.5")

    private var backendFailure: Exception? = null

    private var backendHangs = false

    private var backendCalls = 0

    private val api = object : BittensorSubnetCatalogueApi {
        override suspend fun subnets(url: String): Response<BittensorSubnetCatalogueRemote> {
            backendCalls++
            backendFailure?.let { throw it }
            if (backendHangs) awaitCancellation()
            return Response.success(backendResponse)
        }
    }

    private val repository = RealBittensorPortfolioPriceRepository(BittensorSubnetCatalogueRemoteSource(api, urls), runtime, logger)

    private val subnet = BittensorNetuid.Subnet(1)

    private val missing = BittensorNetuid.Subnet(2)

    @Test
    fun `complete fresh backend skips runtime`() = runTest {
        assertEquals(mapOf(subnet to BigInteger.valueOf(500_000_000)), repository.prices("chain", setOf(subnet)))
        verifyNoInteractions(runtime)
    }

    @Test
    fun `batch runtime fills missing held prices without replacing backend or adding unheld ids`() = runTest {
        `when`(runtime.prices("chain")).thenReturn(mapOf(subnet to BigInteger.ONE, missing to BigInteger.TEN, BittensorNetuid.Subnet(3) to BigInteger.ONE))

        val prices = repository.prices("chain", setOf(subnet, missing))

        assertEquals(mapOf(subnet to BigInteger.valueOf(500_000_000), missing to BigInteger.TEN), prices)
        verify(runtime).prices("chain")
    }

    @Test
    fun `backend timeout leaves a separate runtime fallback budget`() = runTest {
        backendHangs = true
        `when`(runtime.prices("chain")).thenReturn(mapOf(subnet to BigInteger.TEN))

        assertEquals(mapOf(subnet to BigInteger.TEN), repository.prices("chain", setOf(subnet)))
        verify(runtime).prices("chain")
    }

    @Test
    fun `missing backend price field does not discard another fresh price`() = runTest {
        val present = BittensorSubnetCatalogueRemote.Subnet(1, "0.5")
        val absent = BittensorSubnetCatalogueRemote.Subnet(2, null)
        backendResponse = BittensorSubnetCatalogueRemote(listOf(present, absent), backendResponse.meta)
        `when`(runtime.prices("chain")).thenReturn(mapOf(missing to BigInteger.TEN))

        assertEquals(mapOf(subnet to BigInteger.valueOf(500_000_000), missing to BigInteger.TEN), repository.prices("chain", setOf(subnet, missing)))
    }

    @Test
    fun `backend error or stale metadata uses runtime`() = runTest {
        `when`(runtime.prices("chain")).thenReturn(mapOf(subnet to BigInteger.TEN))
        backendFailure = IllegalStateException("Offline")
        assertEquals(mapOf(subnet to BigInteger.TEN), repository.prices("chain", setOf(subnet)))
        backendFailure = null
        backendResponse = response("0.5", freshness = "STALE")
        assertEquals(mapOf(subnet to BigInteger.TEN), repository.prices("chain", setOf(subnet)))
    }

    @Test
    fun `runtime failure retains partial backend and both failures return unknown prices`() = runTest {
        `when`(runtime.prices("chain")).thenThrow(IllegalStateException("Offline"))
        assertEquals(mapOf(subnet to BigInteger.valueOf(500_000_000)), repository.prices("chain", setOf(subnet, missing)))
        backendFailure = IllegalStateException("Offline")
        assertTrue(repository.prices("chain", setOf(subnet)).isEmpty())
    }

    @Test
    fun `cancellation never starts fallback and empty holdings require neither source`() = runTest {
        assertTrue(repository.prices("chain", emptySet()).isEmpty())
        assertEquals(0, backendCalls)
        backendFailure = CancellationException("Cancelled")
        assertTrue(runCatching { repository.prices("chain", setOf(subnet)) }.exceptionOrNull() is CancellationException)
        verifyNoInteractions(runtime)
    }

    @Test
    fun `runtime cancellation is not converted to a partial success`() = runTest {
        `when`(runtime.prices("chain")).thenThrow(CancellationException("Cancelled"))
        assertTrue(runCatching { repository.prices("chain", setOf(subnet, missing)) }.exceptionOrNull() is CancellationException)
    }

    @Test
    fun `backend decimal price is converted to protocol units exactly`() {
        val prices = mapBittensorPortfolioPrices(response("0.006571125"))

        assertEquals(mapOf(BittensorNetuid.Subnet(1) to BigInteger.valueOf(6_571_125)), prices)
    }

    @Test
    fun `zero price is unavailable rather than a zero valuation`() {
        assertTrue(mapBittensorPortfolioPrices(response("0")).isEmpty())
    }

    @Test
    fun `stale prices cannot value holdings but are still a listing price`() {
        assertTrue(mapBittensorPortfolioPrices(response("0.5", freshness = "STALE")).isEmpty())
        assertEquals(
            mapOf(BittensorNetuid.Subnet(1) to BigInteger.valueOf(500_000_000)),
            mapBittensorSubnetPrices(response("0.5", freshness = "STALE"))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unavailable component cannot supply prices`() {
        mapBittensorPortfolioPrices(response("0.5", availability = "UNAVAILABLE"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative prices are rejected`() {
        mapBittensorPortfolioPrices(response("-0.5"))
    }

    @Test(expected = ArithmeticException::class)
    fun `sub atomic prices are not silently rounded`() {
        mapBittensorPortfolioPrices(response("0.0000000001"))
    }

    private fun response(
        price: String,
        freshness: String = "FRESH",
        availability: String = "AVAILABLE"
    ): BittensorSubnetCatalogueRemote {
        val subnet = BittensorSubnetCatalogueRemote.Subnet(1, price)
        val component = BittensorRecommendationsRemote.Component(availability, freshness)
        val components = BittensorSubnetCatalogueRemote.Components(component)
        val meta = BittensorSubnetCatalogueRemote.Meta(components)
        return BittensorSubnetCatalogueRemote(listOf(subnet), meta)
    }
}
