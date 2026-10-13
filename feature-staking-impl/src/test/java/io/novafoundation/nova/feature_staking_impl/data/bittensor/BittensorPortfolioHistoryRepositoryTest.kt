package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioHistoryPeriod
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.ss58.SS58Encoder.toAddress
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import retrofit2.HttpException
import retrofit2.Response

class BittensorPortfolioHistoryRepositoryTest {

    private val chain = mock(Chain::class.java).also {
        `when`(it.addressPrefix).thenReturn(42)
        `when`(it.isEthereumBased).thenReturn(false)
    }
    private val coldkey = AccountIdKey(ByteArray(32) { 7 })

    private class Api(private val response: Response<BittensorPortfolioHistoryRemote>) : BittensorPortfolioHistoryApi {
        val requests = mutableListOf<Pair<String, BittensorPortfolioHistoryRequest>>()

        override suspend fun history(url: String, body: BittensorPortfolioHistoryRequest): Response<BittensorPortfolioHistoryRemote> {
            requests += url to body
            return response
        }
    }

    private fun valid() = BittensorPortfolioHistoryRemote(
        "THIRTY_DAYS",
        BittensorPortfolioHistoryRemote.Window("2026-08-03T14:00:00Z", "2026-09-02T14:00:00Z"),
        listOf(BittensorPortfolioHistoryRemote.Point("2026-09-02T12:00:00Z", "1", "2", true)),
        "TAO_APP_PARTIAL",
        BittensorPortfolioHistoryRemote.Meta("COMPLETE")
    )

    @Test
    fun `posts the generic ss58 coldkey and the preset to the history route`() = runBlocking {
        val api = Api(Response.success(valid()))
        val history = RealBittensorPortfolioHistoryRepository(api, "https://infra.test/").history(chain, coldkey, BittensorPortfolioHistoryPeriod.THIRTY_DAYS)

        val (url, body) = api.requests.single()
        assertEquals("https://infra.test/v1/bittensor/portfolio/history/search", url)
        assertEquals(coldkey.value.toAddress(42), body.accountSubject)
        assertTrue(body.accountSubject.startsWith("5"))
        assertEquals("THIRTY_DAYS", body.period)
        assertEquals(1, history.points.size)
    }

    @Test
    fun `non success status surfaces as an http exception`() = runBlocking {
        val api = Api(Response.error(503, "{\"error\":\"unavailable\"}".toResponseBody("application/json".toMediaType())))
        val result = runCatching { RealBittensorPortfolioHistoryRepository(api, "https://infra.test/").history(chain, coldkey, BittensorPortfolioHistoryPeriod.ONE_DAY) }
        assertTrue(result.exceptionOrNull() is HttpException)
    }

    @Test
    fun `contract violation in the body fails the request`() = runBlocking {
        val broken = BittensorPortfolioHistoryRemote("ONE_DAY", valid().window, emptyList(), "FULL", BittensorPortfolioHistoryRemote.Meta("COMPLETE"))
        val api = Api(Response.success(broken))
        val result = runCatching { RealBittensorPortfolioHistoryRepository(api, "https://infra.test/").history(chain, coldkey, BittensorPortfolioHistoryPeriod.ONE_DAY) }
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
