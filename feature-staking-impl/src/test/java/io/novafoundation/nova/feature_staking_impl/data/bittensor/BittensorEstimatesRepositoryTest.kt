package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class BittensorEstimatesRepositoryTest {

    private val api = EstimatesApi()

    private val infrastructureUrls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }

    private val chain = mock(Chain::class.java)

    private val remoteSource = BittensorEstimatesRemoteSource(api, spacingMillis = 0L)

    private val computationalCache = mock(ComputationalCache::class.java)

    private val historyRepository = BittensorOperationHistoryRepository(
        remoteSource,
        infrastructureUrls,
        computationalCache,
        mock(Preferences::class.java)
    )

    private val yieldRepository = BittensorAlphaYieldRepository(remoteSource, infrastructureUrls)

    private val address = "test-account"

    @Test
    fun `history stops at the last page and caches the completed result`() = runBlocking {
        api.nextHistoryPage = { if (it == 1) 2 else null }

        val first = history()
        val second = history()

        assertTrue(first.allProviderPagesRead)
        assertSame(first, second)
        assertEquals(listOf(1, 2), api.historyPages)
    }

    @Test
    fun `own trade invalidates otherwise fresh history`() = runBlocking {
        history()
        historyRepository.ownTradeSubmitted(requireNotNull(historyRepository.request(address)))
        history()

        assertEquals(listOf(1, 1), api.historyPages)
    }

    @Test
    fun `history capped at ten pages remains incomplete`() = runBlocking {
        api.nextHistoryPage = { it + 1 }

        val history = history()

        assertFalse(history.allProviderPagesRead)
        assertEquals(
            (1..10)
                .toList(),
            api.historyPages
        )
    }

    @Test
    fun `the shortest cache lifetime across pages controls the history cache`() = runBlocking {
        api.nextHistoryPage = { if (it == 1) 2 else null }
        api.historyCacheControl = { if (it == 2) "no-store" else "max-age=60" }

        history()
        history()

        assertEquals(
            listOf(
                1,
                2,
                1,
                2
            ),
            api.historyPages
        )
    }

    @Test
    fun `unavailable history is rejected and never cached while stale history is served`() = runBlocking {
        api.availability = "UNAVAILABLE"
        assertTrue(
            runCatching { history() }
                .isFailure
        )
        api.availability = "AVAILABLE"
        api.freshness = "STALE"

        assertTrue(
            history()
                .allProviderPagesRead
        )
        assertEquals(listOf(1, 1), api.historyPages)
    }

    @Test
    fun `repeated and out of range pagination are rejected`() = runBlocking {
        api.nextHistoryPage = { it }
        assertTrue(
            runCatching { history() }
                .isFailure
        )
        api.nextHistoryPage = { 101 }
        assertTrue(
            runCatching { history() }
                .isFailure
        )
    }

    @Test
    fun `cancelled history request does not cache partial data`() = runBlocking {
        api.failure = CancellationException("Cancelled")
        assertTrue(
            runCatching { history() }
                .exceptionOrNull() is CancellationException
        )
        api.failure = null

        assertTrue(
            history()
                .allProviderPagesRead
        )
        assertEquals(listOf(1, 1), api.historyPages)
    }

    @Test
    fun `skipped history page is rejected and a corrected response can be retried`() = runBlocking {
        api.nextHistoryPage = { 3 }

        assertTrue(runCatching { history() }.isFailure)
        assertEquals(listOf(1), api.historyPages)

        api.nextHistoryPage = { null }
        assertTrue(history().allProviderPagesRead)
        assertEquals(listOf(1, 1), api.historyPages)
    }

    @Test
    fun `missing and incorrect history page sizes are rejected`() = runBlocking {
        for (pageSize in listOf(null, 0, 50, 101)) {
            api.historyPageSize = pageSize

            assertTrue(runCatching { history() }.isFailure)
        }

        api.historyPageSize = 100
        assertTrue(history().allProviderPagesRead)
    }

    @Test
    fun `oversized history page is rejected before mapping items`() = runBlocking {
        val item = mock(BittensorOperationsRemote.Item::class.java)
        api.historyItems = List(101) { item }

        val failure = runCatching { history() }.exceptionOrNull()

        assertEquals("Operation history page exceeds its declared size", failure?.message)
    }

    @Test
    fun `history cache lifetime begins before the response is received`() = runBlocking {
        val baseUrl = requireNotNull(infrastructureUrls.baseUrl())
        val result = remoteSource.history(baseUrl, address)

        assertTrue(result.validUntilNanos <= api.historyResponseAtNanos + 60_000_000_000L)
    }

    @Test
    fun `yield cache lifetime begins before the response is received`() = runBlocking {
        val baseUrl = requireNotNull(infrastructureUrls.baseUrl())
        val subnetId = BittensorNetuid.Subnet(1)
        val result = remoteSource.yields(baseUrl, chain, subnetId)

        assertTrue(result.validUntilNanos <= api.yieldResponseAtNanos + 60_000_000_000L)
    }

    @Test
    fun `yield snapshot retains the first valid rate per hotkey without percent conversion`() = runBlocking {
        val address = chain.addressOf(ByteArray(32) { 2 })
        api.yieldItems = {
            listOf(
                BittensorAlphaYieldsRemote.Item(
                    1,
                    address,
                    "12.5",
                    "ALPHA_VALIDATOR_APY"
                ),
                BittensorAlphaYieldsRemote.Item(1, address, "99", "ALPHA_VALIDATOR_APY"),
                BittensorAlphaYieldsRemote.Item(
                    1,
                    "invalid",
                    "10",
                    "ALPHA_VALIDATOR_APY"
                )
            )
        }

        val yields = yieldRepository.alphaYields(chain, BittensorNetuid.Subnet(1))

        assertEquals(listOf(BigDecimal("12.5")), yields.values.toList())
        assertEquals(listOf("https://infra.test/v1/bittensor/subnets/1/yields/alpha"), api.yieldRequests)
    }

    @Test
    fun `yield snapshot does not inherit operation history throttling`() = runTest {
        val throttledSource = BittensorEstimatesRemoteSource(api, spacingMillis = 12_000L)
        val baseUrl = requireNotNull(infrastructureUrls.baseUrl())

        val pages = throttledSource.yields(baseUrl, chain, BittensorNetuid.Subnet(1))

        assertEquals(listOf("https://infra.test/v1/bittensor/subnets/1/yields/alpha"), api.yieldRequests)
        assertEquals(0L, testScheduler.currentTime)
        assertTrue(pages.allPagesRead)
    }

    @Test
    fun `operation history retains its page throttling`() = runTest {
        val throttledSource = BittensorEstimatesRemoteSource(api, spacingMillis = 12_000L)
        val baseUrl = requireNotNull(infrastructureUrls.baseUrl())
        api.nextHistoryPage = { if (it == 1) 2 else null }

        val pages = throttledSource.history(baseUrl, address)

        assertEquals(listOf(1, 2), api.historyPages)
        assertEquals(12_000L, testScheduler.currentTime)
        assertTrue(pages.allPagesRead)
    }

    @Test
    fun `unreliable first row is retained only for explicit unfiltered browsing`() = runBlocking {
        val address = chain.addressOf(ByteArray(32) { 2 })
        api.yieldItems = {
            listOf(
                BittensorAlphaYieldsRemote.Item(
                    1,
                    address,
                    "669990.45",
                    "ALPHA_VALIDATOR_APY",
                    rateReliability = "UNRELIABLE"
                ),
                BittensorAlphaYieldsRemote.Item(
                    1,
                    address,
                    "52.1",
                    "ALPHA_VALIDATOR_APY",
                    rateReliability = "RELIABLE"
                )
            )
        }

        val yields = yieldRepository.alphaYields(chain, BittensorNetuid.Subnet(1))

        assertTrue(yields.isEmpty())
        val allYields = yieldRepository.alphaYields(chain, BittensorNetuid.Subnet(1), includeUnreliable = true)
        assertEquals(listOf(BigDecimal("669990.45")), allYields.values.toList())
    }

    @Test
    fun `yield snapshot is loaded with a single request`() = runBlocking {
        yieldRepository.alphaYields(chain, BittensorNetuid.Subnet(1))

        assertEquals(listOf("https://infra.test/v1/bittensor/subnets/1/yields/alpha"), api.yieldRequests)
    }

    @Test
    fun `unexpected yield metric rejects the response`() = runBlocking {
        api.yieldItems = {
            listOf(
                BittensorAlphaYieldsRemote.Item(
                    1,
                    null,
                    "10",
                    "OTHER"
                )
            )
        }

        assertTrue(
            runCatching { yieldRepository.alphaYields(chain, BittensorNetuid.Subnet(1)) }
                .isFailure
        )
    }

    @Test
    fun `stale yield snapshot remains displayable but recommendations exclude unreliable rates`() = runBlocking {
        api.freshness = "STALE"
        val address = chain.addressOf(ByteArray(32) { 2 })
        api.yieldItems = {
            listOf(BittensorAlphaYieldsRemote.Item(1, address, "12.5", "ALPHA_VALIDATOR_APY", rateReliability = "RELIABLE"))
        }
        assertEquals(listOf(BigDecimal("12.5")), yieldRepository.alphaYields(chain, BittensorNetuid.Subnet(1)).values.toList())
    }

    @Test
    fun `failed yield refresh preserves cached rates without refreshing their expiry`() = runBlocking {
        api.yieldCacheControl = "max-age=0"
        val address = chain.addressOf(ByteArray(32) { 2 })
        api.yieldItems = {
            listOf(BittensorAlphaYieldsRemote.Item(1, address, "12.5", "ALPHA_VALIDATOR_APY"))
        }
        val first = yieldRepository.alphaYields(chain, BittensorNetuid.Subnet(1))
        api.failure = java.io.IOException("offline")
        repeat(2) {
            assertEquals(first, yieldRepository.alphaYields(chain, BittensorNetuid.Subnet(1)))
        }
        assertEquals(3, api.yieldRequests.size)
    }

    private suspend fun history(): io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationHistory {
        val request = requireNotNull(historyRepository.request(address))
        return historyRepository.history(request)
    }

    internal class EstimatesApi : BittensorEstimatesApi {

        var beforeHistoryResponse: suspend () -> Unit = {}

        val historyPages = mutableListOf<Int>()

        val yieldRequests = mutableListOf<String>()

        var historyPageSize: Int? = 100

        var historyItems = emptyList<BittensorOperationsRemote.Item>()

        var historyResponseAtNanos = 0L

        var yieldResponseAtNanos = 0L

        var nextHistoryPage: (Int) -> Int? = { null }

        var historyCacheControl: (Int) -> String = { "max-age=60" }

        var yieldItems: () -> List<BittensorAlphaYieldsRemote.Item> = { emptyList() }

        var freshness = "FRESH"

        var availability = "AVAILABLE"

        var yieldCacheControl = "max-age=60"

        var failure: Exception? = null

        override suspend fun operations(url: String, address: String, page: Int): Response<BittensorOperationsRemote> {
            historyPages += page
            beforeHistoryResponse()
            failure?.let { throw it }
            val component = BittensorRecommendationsRemote.Component(availability, freshness)
            val components = BittensorOperationsRemote.Components(component)
            val meta = BittensorOperationsRemote.Meta(components)
            val pageInfo = BittensorHistoryPageInfo(page, nextHistoryPage(page), historyPageSize)
            val body = BittensorOperationsRemote(
                historyItems,
                "TAO_APP_PARTIAL",
                meta,
                pageInfo
            )
            val headers = Headers.headersOf("Cache-Control", historyCacheControl(page))
            historyResponseAtNanos = System.nanoTime()
            return Response.success(body, headers)
        }

        override suspend fun alphaYields(url: String): Response<BittensorAlphaYieldsRemote> {
            yieldRequests += url
            failure?.let { throw it }
            val component = BittensorRecommendationsRemote.Component("AVAILABLE", freshness)
            val components = BittensorAlphaYieldsRemote.Components(component)
            val meta = BittensorAlphaYieldsRemote.Meta(components)
            val body = BittensorAlphaYieldsRemote(yieldItems(), meta)
            val headers = Headers.headersOf("Cache-Control", yieldCacheControl)
            yieldResponseAtNanos = System.nanoTime()
            return Response.success(body, headers)
        }
    }
}
