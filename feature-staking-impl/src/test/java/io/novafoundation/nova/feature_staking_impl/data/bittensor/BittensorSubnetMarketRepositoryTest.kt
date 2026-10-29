package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import java.math.BigDecimal
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorSubnetMarketRepositoryTest {

    @Test
    fun `market ids use netuid symbols rather than names or alpha symbols`() {
        val rows = listOf(
            BittensorSubnetMarketRemote("vanta", " SN8 "),
            BittensorSubnetMarketRemote("wrong", "theta"),
            BittensorSubnetMarketRemote("root", "sn0"),
            BittensorSubnetMarketRemote("overflow", "sn65536"),
            BittensorSubnetMarketRemote("last-subnet", "sn65535"),
            BittensorSubnetMarketRemote("invalid/id", "sn9"),
            BittensorSubnetMarketRemote("duplicate", "sn8"),
            BittensorSubnetMarketRemote(id = null, "sn10")
        )

        val expected = mapOf(BittensorNetuid.Subnet(8) to "vanta", BittensorNetuid.Subnet(65535) to "last-subnet")
        assertEquals(expected, mapBittensorMarketIds(rows))
    }

    @Test
    fun `markets listing no subnet fail and are not cached as unlisted`() = runTest {
        val api = MarketsApi(listOf(BittensorSubnetMarketRemote("wrong", "theta")))
        val repository = BittensorSubnetMarketRepository(api)

        val failure = runCatching { repository.weeklyMarkets() }.exceptionOrNull()
        api.rows = listOf(BittensorSubnetMarketRemote("vanta", "sn8"))

        assertTrue(failure is IllegalArgumentException)
        assertEquals("vanta", repository.priceId(BittensorNetuid.Subnet(8)))
        assertEquals(2, api.requests)
    }

    @Test
    fun `normalized price id and weekly statistics belong to the same first subnet row`() = runTest {
        val first = BittensorSubnetMarketRemote(" vanta ", " SN8 ", weekChangePercent = BigDecimal.ONE)
        val duplicate = BittensorSubnetMarketRemote("another-market", "sn8", weekChangePercent = BigDecimal.TEN)
        val api = MarketsApi(listOf(first, duplicate))
        val repository = BittensorSubnetMarketRepository(api)
        val subnetId = BittensorNetuid.Subnet(8)

        assertEquals("vanta", repository.priceId(subnetId))
        assertEquals(first.copy(id = "vanta"), repository.weeklyMarkets()[subnetId])
        assertEquals(1, api.requests)
    }

    @Test
    fun `repeated market id cannot replace statistics with another subnet row`() = runTest {
        val api = MarketsApi(listOf(BittensorSubnetMarketRemote("vanta", "sn8"), BittensorSubnetMarketRemote("vanta", "sn9")))
        val repository = BittensorSubnetMarketRepository(api)

        assertTrue(runCatching { repository.weeklyMarkets() }.exceptionOrNull() is IllegalArgumentException)
        api.rows = listOf(BittensorSubnetMarketRemote("vanta", "sn8"))
        assertEquals("vanta", repository.priceId(BittensorNetuid.Subnet(8)))
        assertEquals(2, api.requests)
    }

    @Test
    fun `full page is followed by the requested next page before caching`() = runTest {
        val api = MarketsApi(rows = emptyList())
        api.pages = mapOf(1 to page(1), 2 to listOf(BittensorSubnetMarketRemote("last-market", "sn251")))
        val repository = BittensorSubnetMarketRepository(api)

        assertEquals("last-market", repository.priceId(BittensorNetuid.Subnet(251)))
        assertEquals(251, repository.weeklyMarkets().size)
        assertEquals(listOf(1, 2), api.requestedPages)
    }

    @Test
    fun `full final page is incomplete and must not cache omitted subnets as unlisted`() = runTest {
        val api = MarketsApi(rows = emptyList())
        api.pages = (1..4).associateWith { page(it) }
        val repository = BittensorSubnetMarketRepository(api)

        assertTrue(runCatching { repository.weeklyMarkets() }.exceptionOrNull() is IllegalStateException)
        api.pages = null
        api.rows = listOf(BittensorSubnetMarketRemote("recovered", "sn1"))
        assertEquals("recovered", repository.priceId(BittensorNetuid.Subnet(1)))
        assertEquals(listOf(1, 2, 3, 4, 1), api.requestedPages)
    }

    @Test
    fun `repeated and oversized pages fail without cache admission`() = runTest {
        for (pages in listOf(mapOf(1 to page(1), 2 to page(1)), mapOf(1 to page(1) + page(2).first()))) {
            val api = MarketsApi(rows = emptyList())
            api.pages = pages
            val repository = BittensorSubnetMarketRepository(api)

            assertTrue(runCatching { repository.weeklyMarkets() }.exceptionOrNull() is IllegalArgumentException)
            api.pages = null
            api.rows = listOf(BittensorSubnetMarketRemote("recovered", "sn1"))
            assertEquals("recovered", repository.priceId(BittensorNetuid.Subnet(1)))
        }
    }

    @Test
    fun `cache expires at the request start deadline including when the clock starts at zero`() = runTest {
        var now = 0L
        val api = MarketsApi(listOf(BittensorSubnetMarketRemote("vanta", "sn8")))
        val repository = BittensorSubnetMarketRepository(
            api,
            nanoTime = { now }
        )
        val subnetId = BittensorNetuid.Subnet(8)

        repository.priceId(subnetId)
        now = 5.minutes.inWholeNanoseconds - 1
        repository.weeklyMarkets()
        assertEquals(1, api.requests)

        now++
        api.rows = listOf(BittensorSubnetMarketRemote("updated", "sn8"))
        assertEquals("updated", repository.priceId(subnetId))
        assertEquals(2, api.requests)
    }

    @Test
    fun `slow transport does not extend cache freshness`() = runTest {
        var now = 0L
        val api = MarketsApi(listOf(BittensorSubnetMarketRemote("vanta", "sn8")))
        api.beforeResponse = { now += 5.minutes.inWholeNanoseconds }
        val repository = BittensorSubnetMarketRepository(
            api,
            nanoTime = { now }
        )

        repository.weeklyMarkets()
        repository.weeklyMarkets()

        assertEquals(2, api.requests)
    }

    @Test
    fun `timeout cancellation and transport failure release the lock without caching failure`() = runTest {
        val api = MarketsApi(listOf(BittensorSubnetMarketRemote("vanta", "sn8")))
        val repository = BittensorSubnetMarketRepository(api)
        api.beforeResponse = { awaitCancellation() }
        assertTrue(runCatching { repository.weeklyMarkets() }.exceptionOrNull() is BittensorTimeoutException)
        assertTrue(runCatching { withTimeout(1L) { repository.weeklyMarkets() } }.exceptionOrNull() is TimeoutCancellationException)

        api.beforeResponse = { throw CancellationException("Cancelled") }
        assertTrue(runCatching { repository.weeklyMarkets() }.exceptionOrNull() is CancellationException)
        api.beforeResponse = { throw IllegalStateException("Offline") }
        assertTrue(runCatching { repository.weeklyMarkets() }.exceptionOrNull() is IllegalStateException)

        api.beforeResponse = {}
        assertEquals("vanta", repository.priceId(BittensorNetuid.Subnet(8)))
        assertEquals(5, api.requests)
    }

    private fun page(number: Int): List<BittensorSubnetMarketRemote> = (1..250).map { offset ->
        val netuid = (number - 1) * 250 + offset
        BittensorSubnetMarketRemote("market-$netuid", "sn$netuid")
    }

    private class MarketsApi(var rows: List<BittensorSubnetMarketRemote>) : BittensorSubnetMarketsApi {

        val requestedPages = mutableListOf<Int>()

        val requests: Int
            get() = requestedPages.size

        var pages: Map<Int, List<BittensorSubnetMarketRemote>>? = null

        var beforeResponse: suspend () -> Unit = {}

        override suspend fun markets(
            category: String,
            currency: String,
            priceChangePercentage: String,
            sparkline: Boolean,
            perPage: Int,
            page: Int
        ): List<BittensorSubnetMarketRemote> {
            requestedPages.add(page)
            beforeResponse()
            return pages?.getValue(page) ?: rows
        }
    }
}
