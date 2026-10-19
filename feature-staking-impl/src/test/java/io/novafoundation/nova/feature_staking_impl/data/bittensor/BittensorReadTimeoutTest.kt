package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import retrofit2.Response

class BittensorReadTimeoutTest {

    @Test
    fun `market timeout is recoverable and does not cache failure or retain mutex`() = runTest {
        val api = MarketsApi()
        val repository = BittensorSubnetMarketRepository(api)

        val failure = runCatching { repository.weeklyMarkets() }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals("Subnet markets timed out", failure?.message)

        api.hang = false
        assertEquals("subnet-one", repository.priceId(BittensorNetuid.Subnet(1)))
        assertEquals(2, api.requests)
    }

    @Test
    fun `caller timeout remains cancellation in market source and releases mutex`() = runTest {
        val api = MarketsApi()
        val repository = BittensorSubnetMarketRepository(api)

        val failure = runCatching { withTimeout(100) { repository.weeklyMarkets() } }.exceptionOrNull()
        assertTrue(failure is CancellationException)

        api.hang = false
        assertEquals("subnet-one", repository.priceId(BittensorNetuid.Subnet(1)))
    }

    @Test
    fun `history page timeout is recoverable and next request succeeds`() = runTest {
        val delegate = BittensorEstimatesRepositoryTest.EstimatesApi()
        val api = EstimatesApi(delegate)
        val source = BittensorEstimatesRemoteSource(api, spacingMillis = 0)
        val origin = "https://infra.test/".toHttpUrl()

        val failure = runCatching { source.history(origin, "account") }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals("Operation history page timed out", failure?.message)

        api.hang = false
        assertTrue(source.history(origin, "account").allPagesRead)
    }

    @Test
    fun `yield page timeout is recoverable and next request succeeds`() = runTest {
        val delegate = BittensorEstimatesRepositoryTest.EstimatesApi()
        val api = EstimatesApi(delegate)
        val source = BittensorEstimatesRemoteSource(api, spacingMillis = 0)
        val origin = "https://infra.test/".toHttpUrl()
        val chain = mock(Chain::class.java)
        val subnet = BittensorNetuid.Subnet(1)

        val failure = runCatching { source.yields(origin, chain, subnet) }.exceptionOrNull()
        assertTrue(failure is IllegalStateException)
        assertEquals("Validator yield page timed out", failure?.message)

        api.hang = false
        assertTrue(source.yields(origin, chain, subnet).allPagesRead)
    }

    @Test
    fun `caller timeout remains cancellation in both estimate endpoints`() = runTest {
        val delegate = BittensorEstimatesRepositoryTest.EstimatesApi()
        val api = EstimatesApi(delegate)
        val source = BittensorEstimatesRemoteSource(api, spacingMillis = 0)
        val origin = "https://infra.test/".toHttpUrl()
        val chain = mock(Chain::class.java)
        val subnet = BittensorNetuid.Subnet(1)

        val historyFailure = runCatching { withTimeout(100) { source.history(origin, "account") } }.exceptionOrNull()
        val yieldFailure = runCatching { withTimeout(100) { source.yields(origin, chain, subnet) } }.exceptionOrNull()

        assertTrue(historyFailure is CancellationException)
        assertTrue(yieldFailure is CancellationException)
    }

    private class MarketsApi : BittensorSubnetMarketsApi {

        var hang = true

        var requests = 0

        override suspend fun markets(
            category: String,
            currency: String,
            priceChangePercentage: String,
            sparkline: Boolean,
            perPage: Int,
            page: Int
        ): List<BittensorSubnetMarketRemote> {
            requests++
            if (hang) awaitCancellation()
            return listOf(BittensorSubnetMarketRemote("subnet-one", "sn1"))
        }
    }

    private class EstimatesApi(private val delegate: BittensorEstimatesApi) : BittensorEstimatesApi {

        var hang = true

        override suspend fun operations(url: String, address: String, page: Int): Response<BittensorOperationsRemote> {
            if (hang) awaitCancellation()
            return delegate.operations(url, address, page)
        }

        override suspend fun alphaYields(url: String, page: Int, pageSize: Int): Response<BittensorAlphaYieldsRemote> {
            if (hang) awaitCancellation()
            return delegate.alphaYields(url, page, pageSize)
        }
    }
}
