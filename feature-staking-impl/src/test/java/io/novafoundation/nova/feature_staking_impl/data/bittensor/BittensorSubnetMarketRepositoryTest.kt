package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import kotlinx.coroutines.test.runTest
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
            BittensorSubnetMarketRemote(null, "sn10")
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

    private class MarketsApi(var rows: List<BittensorSubnetMarketRemote>) : BittensorSubnetMarketsApi {

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
            return rows
        }
    }
}
