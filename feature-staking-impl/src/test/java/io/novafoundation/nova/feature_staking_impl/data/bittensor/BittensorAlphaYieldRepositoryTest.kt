package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class BittensorAlphaYieldRepositoryTest {

    private val api = BittensorEstimatesRepositoryTest.EstimatesApi()

    private var baseUrl = "https://infra.test/".toHttpUrl()

    private val infrastructureUrls = InfrastructureUrls { baseUrl }

    private val chain = mock(Chain::class.java)

    private val remoteSource = BittensorEstimatesRemoteSource(api, spacingMillis = 0L)

    private val repository = BittensorAlphaYieldRepository(remoteSource, infrastructureUrls)

    private val subnet = BittensorNetuid.Subnet(value = 1)

    private val hotkey = AccountIdKey(ByteArray(32) { 2 })

    @Test
    fun `fresh cache retains the first hotkey row across reliability filter changes`() = runBlocking {
        api.yieldItems = {
            listOf(
                yieldItem(reportedRate = "12.500000000000000001", rateReliability = "UNRELIABLE"),
                yieldItem(reportedRate = "99")
            )
        }
        val expected = mapOf(hotkey to BigDecimal("12.500000000000000001"))

        assertTrue(repository.alphaYields(chain, subnet).isEmpty())
        api.failure = IOException("Must use the fresh cache")

        assertEquals(expected, repository.alphaYields(chain, subnet, includeUnreliable = true))
        assertTrue(repository.alphaYields(chain, subnet).isEmpty())
        assertEquals(expected, repository.alphaYields(chain, subnet, includeUnreliable = true))
        assertEquals(listOf("https://infra.test/v1/bittensor/subnets/1/yields/alpha"), api.yieldRequests)
    }

    @Test
    fun `expired cache is replaced by a successful refresh with its own lifetime`() = runBlocking {
        api.yieldCacheControl = "max-age=0"
        api.yieldItems = { listOf(yieldItem(reportedRate = "12.5")) }
        assertEquals(mapOf(hotkey to BigDecimal("12.5")), repository.alphaYields(chain, subnet))

        api.yieldCacheControl = "max-age=60"
        api.yieldItems = { listOf(yieldItem(reportedRate = "20.25")) }
        val expected = mapOf(hotkey to BigDecimal("20.25"))
        assertEquals(expected, repository.alphaYields(chain, subnet))
        api.failure = IOException("Must use the refreshed cache")

        assertEquals(expected, repository.alphaYields(chain, subnet))
        assertEquals(2, api.yieldRequests.size)
    }

    @Test
    fun `stale fallback retains reliability choices and retries until a refresh succeeds`() = runBlocking {
        api.yieldCacheControl = "max-age=0"
        api.yieldItems = { listOf(yieldItem(reportedRate = "12.5", rateReliability = "UNRELIABLE")) }
        val first = repository.alphaYields(chain, subnet, includeUnreliable = true)
        assertEquals(mapOf(hotkey to BigDecimal("12.5")), first)

        api.failure = IOException("offline")
        assertTrue(repository.alphaYields(chain, subnet).isEmpty())
        assertEquals(first, repository.alphaYields(chain, subnet, includeUnreliable = true))
        assertEquals(3, api.yieldRequests.size)

        api.failure = null
        api.yieldItems = { listOf(yieldItem(reportedRate = "25.75")) }

        assertEquals(mapOf(hotkey to BigDecimal("25.75")), repository.alphaYields(chain, subnet))
        assertEquals(4, api.yieldRequests.size)
    }

    @Test
    fun `failure without cached yields propagates and does not prevent retry`() = runBlocking {
        api.failure = IOException("offline")

        repeat(2) {
            val failure = runCatching { repository.alphaYields(chain, subnet) }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertEquals("offline", failure?.message)
        }
        api.failure = null
        api.yieldItems = { listOf(yieldItem()) }

        assertEquals(mapOf(hotkey to BigDecimal("12.5")), repository.alphaYields(chain, subnet))
        assertEquals(3, api.yieldRequests.size)
    }

    @Test
    fun `cancellation without cache propagates and leaves the next request usable`() = runBlocking {
        api.failure = CancellationException("Cancelled yield load")

        val failure = runCatching { repository.alphaYields(chain, subnet) }.exceptionOrNull()

        assertTrue(failure is CancellationException)
        assertEquals("Cancelled yield load", failure?.message)
        api.failure = null
        api.yieldItems = { listOf(yieldItem()) }

        assertEquals(mapOf(hotkey to BigDecimal("12.5")), repository.alphaYields(chain, subnet))
        assertEquals(2, api.yieldRequests.size)
    }

    @Test
    fun `cancellation during refresh propagates instead of returning stale cache`() = runBlocking {
        api.yieldCacheControl = "max-age=0"
        api.yieldItems = { listOf(yieldItem()) }
        assertEquals(mapOf(hotkey to BigDecimal("12.5")), repository.alphaYields(chain, subnet))
        api.failure = CancellationException("Cancelled yield refresh")

        val failure = runCatching { repository.alphaYields(chain, subnet) }.exceptionOrNull()

        assertTrue(failure is CancellationException)
        assertEquals("Cancelled yield refresh", failure?.message)
        api.failure = null
        api.yieldItems = { listOf(yieldItem(reportedRate = "30")) }

        assertEquals(mapOf(hotkey to BigDecimal("30")), repository.alphaYields(chain, subnet))
        assertEquals(3, api.yieldRequests.size)
    }

    @Test
    fun `origins have separate cached yields and cannot borrow another origin on failure`() = runBlocking {
        val firstOrigin = baseUrl
        api.yieldItems = { listOf(yieldItem()) }
        val first = repository.alphaYields(chain, subnet)

        baseUrl = "https://other-infra.test/".toHttpUrl()
        api.failure = IOException("Other origin unavailable")
        val failure = runCatching { repository.alphaYields(chain, subnet) }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertEquals("Other origin unavailable", failure?.message)

        api.failure = null
        api.yieldItems = { listOf(yieldItem(reportedRate = "40")) }
        assertEquals(mapOf(hotkey to BigDecimal("40")), repository.alphaYields(chain, subnet))
        baseUrl = firstOrigin

        assertEquals(mapOf(hotkey to BigDecimal("12.5")), first)
        assertEquals(first, repository.alphaYields(chain, subnet))
        assertEquals(
            listOf(
                "https://infra.test/v1/bittensor/subnets/1/yields/alpha",
                "https://other-infra.test/v1/bittensor/subnets/1/yields/alpha",
                "https://other-infra.test/v1/bittensor/subnets/1/yields/alpha"
            ),
            api.yieldRequests
        )
    }

    @Test
    fun `subnets have separate cached yields and cannot borrow another subnet on failure`() = runBlocking {
        api.yieldItems = { listOf(yieldItem()) }
        val first = repository.alphaYields(chain, subnet)
        val otherSubnet = BittensorNetuid.Subnet(value = 2)

        api.failure = IOException("Other subnet unavailable")
        val failure = runCatching { repository.alphaYields(chain, otherSubnet) }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertEquals("Other subnet unavailable", failure?.message)

        api.failure = null
        api.yieldItems = { listOf(yieldItem(otherSubnet, reportedRate = "50")) }
        assertEquals(mapOf(hotkey to BigDecimal("50")), repository.alphaYields(chain, otherSubnet))

        assertEquals(mapOf(hotkey to BigDecimal("12.5")), first)
        assertEquals(first, repository.alphaYields(chain, subnet))
        assertEquals(
            listOf(
                "https://infra.test/v1/bittensor/subnets/1/yields/alpha",
                "https://infra.test/v1/bittensor/subnets/2/yields/alpha",
                "https://infra.test/v1/bittensor/subnets/2/yields/alpha"
            ),
            api.yieldRequests
        )
    }

    @Test
    fun `cache holds 64 subnet snapshots and clears them when admitting the next snapshot`() = runBlocking {
        val expected = mapOf(hotkey to BigDecimal("12.5"))
        for (netuid in 1..64) {
            val cachedSubnet = BittensorNetuid.Subnet(netuid)
            api.yieldItems = { listOf(yieldItem(cachedSubnet)) }
            assertEquals(expected, repository.alphaYields(chain, cachedSubnet))
        }
        assertEquals(expected, repository.alphaYields(chain, subnet))
        assertEquals(64, api.yieldRequests.size)

        val nextSubnet = BittensorNetuid.Subnet(value = 65)
        api.yieldItems = { listOf(yieldItem(nextSubnet)) }
        assertEquals(expected, repository.alphaYields(chain, nextSubnet))
        assertEquals(expected, repository.alphaYields(chain, nextSubnet))
        assertEquals(65, api.yieldRequests.size)

        api.yieldItems = { listOf(yieldItem()) }
        assertEquals(expected, repository.alphaYields(chain, subnet))
        assertEquals(66, api.yieldRequests.size)
    }

    private fun yieldItem(
        subnet: BittensorNetuid.Subnet = this.subnet,
        reportedRate: String = "12.5",
        rateReliability: String = "RELIABLE"
    ): BittensorAlphaYieldsRemote.Item = BittensorAlphaYieldsRemote.Item(
        netuid = subnet.value,
        hotkey = chain.addressOf(hotkey),
        reportedRate = reportedRate,
        metricKind = "ALPHA_VALIDATOR_APY",
        rateReliability = rateReliability
    )
}
