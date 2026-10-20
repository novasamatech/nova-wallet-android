package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import retrofit2.Response

class BittensorEstimatesRepositoryTest {

    private val api = EstimatesApi()

    private val infrastructureUrls = InfrastructureUrls { "https://infra.test/".toHttpUrl() }

    private val chain = mock(Chain::class.java)

    private val remoteSource = BittensorEstimatesRemoteSource(api, spacingMillis = 0L)

    private val historyRepository = BittensorOperationHistoryRepository(remoteSource, infrastructureUrls)

    private val yieldRepository = BittensorAlphaYieldRepository(remoteSource, infrastructureUrls)

    private val address = "test-account"

    @Test
    fun `history stops at the last page and caches the completed result`() = runBlocking {
        api.nextHistoryPage = { if (it == 1) 2 else null }

        val first = historyRepository.history(address, BigInteger.ONE)
        val second = historyRepository.history(address, BigInteger.ONE)

        assertTrue(first.allProviderPagesRead)
        assertSame(first, second)
        assertEquals(listOf(1, 2), api.historyPages)
    }

    @Test
    fun `balance revision invalidates otherwise fresh history`() = runBlocking {
        historyRepository.history(address, BigInteger.ONE)
        historyRepository.history(address, BigInteger.TEN)

        assertEquals(listOf(1, 1), api.historyPages)
    }

    @Test
    fun `history capped at ten pages remains incomplete`() = runBlocking {
        api.nextHistoryPage = { it + 1 }

        val history = historyRepository.history(address)

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

        historyRepository.history(address)
        historyRepository.history(address)

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
    fun `stale history is rejected and never cached`() = runBlocking {
        api.freshness = "STALE"
        assertTrue(
            runCatching { historyRepository.history(address) }
                .isFailure
        )
        api.freshness = "FRESH"

        assertTrue(
            historyRepository.history(address)
                .allProviderPagesRead
        )
        assertEquals(listOf(1, 1), api.historyPages)
    }

    @Test
    fun `repeated and out of range pagination are rejected`() = runBlocking {
        api.nextHistoryPage = { it }
        assertTrue(
            runCatching { historyRepository.history(address) }
                .isFailure
        )
        api.nextHistoryPage = { 101 }
        assertTrue(
            runCatching { historyRepository.history(address) }
                .isFailure
        )
    }

    @Test
    fun `cancelled history request does not cache partial data`() = runBlocking {
        api.failure = CancellationException("Cancelled")
        assertTrue(
            runCatching { historyRepository.history(address) }
                .exceptionOrNull() is CancellationException
        )
        api.failure = null

        assertTrue(
            historyRepository.history(address)
                .allProviderPagesRead
        )
        assertEquals(listOf(1, 1), api.historyPages)
    }

    @Test
    fun `yield pages retain the first valid rate per hotkey without percent conversion`() = runBlocking {
        val address = chain.addressOf(ByteArray(32) { 2 })
        api.nextYieldPage = { if (it == 1) 2 else null }
        api.yieldItems = { page ->
            listOf(
                BittensorAlphaYieldsRemote.Item(
                    1,
                    address,
                    if (page == 1) "12.5" else "99",
                    "ALPHA_VALIDATOR_APY"
                ),
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
        assertEquals(listOf(1, 2), api.yieldPages)
    }

    @Test
    fun `unreliable yields are skipped so the next reliable row of the hotkey wins`() = runBlocking {
        val address = chain.addressOf(ByteArray(32) { 2 })
        api.nextYieldPage = { null }
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

        assertEquals(listOf(BigDecimal("52.1")), yields.values.toList())
    }

    @Test
    fun `yield loading preserves the three page limit`() = runBlocking {
        api.nextYieldPage = { it + 1 }

        yieldRepository.alphaYields(chain, BittensorNetuid.Subnet(1))

        assertEquals(listOf(1, 2, 3), api.yieldPages)
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

    private class EstimatesApi : BittensorEstimatesApi {

        val historyPages = mutableListOf<Int>()

        val yieldPages = mutableListOf<Int>()

        var nextHistoryPage: (Int) -> Int? = { null }

        var nextYieldPage: (Int) -> Int? = { null }

        var historyCacheControl: (Int) -> String = { "max-age=60" }

        var yieldItems: (Int) -> List<BittensorAlphaYieldsRemote.Item> = { emptyList() }

        var freshness = "FRESH"

        var failure: Exception? = null

        override suspend fun operations(url: String, address: String, page: Int): Response<BittensorOperationsRemote> {
            historyPages += page
            failure?.let { throw it }
            val component = BittensorRecommendationsRemote.Component("AVAILABLE", freshness)
            val components = BittensorOperationsRemote.Components(component)
            val meta = BittensorOperationsRemote.Meta(components)
            val pageInfo = BittensorHistoryPageInfo(page, nextHistoryPage(page))
            val body = BittensorOperationsRemote(
                emptyList(),
                "TAO_APP_PARTIAL",
                meta,
                pageInfo
            )
            val headers = Headers.headersOf("Cache-Control", historyCacheControl(page))
            return Response.success(body, headers)
        }

        override suspend fun alphaYields(url: String, page: Int): Response<BittensorAlphaYieldsRemote> {
            yieldPages += page
            val component = BittensorRecommendationsRemote.Component("AVAILABLE", freshness)
            val components = BittensorAlphaYieldsRemote.Components(component)
            val meta = BittensorAlphaYieldsRemote.Meta(components)
            val pageInfo = BittensorHistoryPageInfo(page, nextYieldPage(page))
            val body = BittensorAlphaYieldsRemote(yieldItems(page), meta, pageInfo)
            val headers = Headers.headersOf("Cache-Control", "max-age=60")
            return Response.success(body, headers)
        }
    }
}
