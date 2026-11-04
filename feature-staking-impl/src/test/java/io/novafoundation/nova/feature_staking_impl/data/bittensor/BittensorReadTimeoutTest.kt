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
        val repository = BittensorSubnetMarketSource(api)

        val failure = runCatching { repository.weeklyMarkets() }.exceptionOrNull()
        assertTrue(failure is BittensorTimeoutException)
        assertEquals("Subnet markets timed out", failure?.message)

        api.hang = false
        assertEquals("subnet-one", repository.priceId(BittensorNetuid.Subnet(1)))
        assertEquals(2, api.requests)
    }

    @Test
    fun `caller timeout remains cancellation in market source and releases mutex`() = runTest {
        val api = MarketsApi()
        val repository = BittensorSubnetMarketSource(api)

        val failure = runCatching { withTimeout(100) { repository.weeklyMarkets() } }.exceptionOrNull()
        assertTrue(failure is CancellationException)

        api.hang = false
        assertEquals("subnet-one", repository.priceId(BittensorNetuid.Subnet(1)))
    }

    @Test
    fun `history page timeout is recoverable and next request succeeds`() = runTest {
        val delegate = BittensorEstimatesRepositoryTest.EstimatesApi()
        val api = EstimatesApi(delegate)
        val backendSource = createBittensorTestSources(scope = backgroundScope, estimatesApi = api)
        val source = backendSource.operations
        val origin = "https://infra.test/".toHttpUrl()

        val failure = runCatching { source.loadHistoryPages(origin, "account", metaAccountId = 1L) }.exceptionOrNull()
        assertTrue(failure is BittensorTimeoutException)
        assertEquals("Backend operations/search timed out", failure?.message)

        api.hang = false
        assertTrue(source.loadHistoryPages(origin, "account", metaAccountId = 1L).allPagesRead)
    }

    @Test
    fun `network only settlement reads are bounded and never seed persistent history`() = runTest {
        val delegate = BittensorEstimatesRepositoryTest.EstimatesApi()
        val api = EstimatesApi(delegate)
        val backendSource = createBittensorTestSources(scope = backgroundScope, estimatesApi = api)
        val source = backendSource.operations
        val origin = "https://infra.test/".toHttpUrl()
        val error = runCatching { source.loadHistoryPages(origin, "account", metaAccountId = 1L, persist = false) }.exceptionOrNull()
        assertTrue(error is BittensorTimeoutException)
        api.hang = false
        assertTrue(source.loadHistoryPages(origin, "account", metaAccountId = 1L, persist = false).allPagesRead)
        org.junit.Assert.assertNull(backendSource.cachedOperationHistory("account", page = 1, metaAccountId = 1L))
    }

    @Test
    fun `yield snapshot timeout is recoverable and next request succeeds`() = runTest {
        val delegate = BittensorEstimatesRepositoryTest.EstimatesApi()
        val api = EstimatesApi(delegate)
        val backendSource = createBittensorTestSources(scope = backgroundScope, estimatesApi = api)
        val origin = "https://infra.test/".toHttpUrl()
        val chain = mock(Chain::class.java)
        val subnet = BittensorNetuid.Subnet(1)

        val failure = runCatching { backendSource.yields.alphaYields(origin, chain, subnet) }.exceptionOrNull()
        assertTrue(failure is BittensorTimeoutException)
        assertEquals("Backend subnets/1/yields/alpha timed out", failure?.message)

        api.hang = false
        assertTrue(requireNotNull(backendSource.yields.alphaYields(origin, chain, subnet).items).isEmpty())
    }

    @Test
    fun `caller timeout remains cancellation in both estimate endpoints`() = runTest {
        val delegate = BittensorEstimatesRepositoryTest.EstimatesApi()
        val api = EstimatesApi(delegate)
        val backendSource = createBittensorTestSources(scope = backgroundScope, estimatesApi = api)
        val source = backendSource.operations
        val origin = "https://infra.test/".toHttpUrl()
        val chain = mock(Chain::class.java)
        val subnet = BittensorNetuid.Subnet(1)

        val historyFailure = runCatching { withTimeout(100) { source.loadHistoryPages(origin, "account", metaAccountId = 1L) } }.exceptionOrNull()
        val yieldFailure = runCatching { withTimeout(100) { backendSource.yields.alphaYields(origin, chain, subnet) } }.exceptionOrNull()

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

        override suspend fun operations(url: String): Response<BittensorOperationsRemote> {
            if (hang) awaitCancellation()
            return delegate.operations(url)
        }

        override suspend fun alphaYields(url: String): Response<BittensorAlphaYieldsRemote> {
            if (hang) awaitCancellation()
            return delegate.alphaYields(url)
        }
    }
}
