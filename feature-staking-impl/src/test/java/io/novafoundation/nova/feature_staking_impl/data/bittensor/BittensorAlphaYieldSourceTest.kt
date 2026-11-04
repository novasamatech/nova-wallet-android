package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import retrofit2.Response

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BittensorAlphaYieldSourceTest {

    private val component = BittensorRecommendationsRemote.Component("AVAILABLE", "FRESH", null)

    private fun <T> response(value: T) = Response.success(value, Headers.headersOf("Cache-Control", "max-age=60"))

    @Test
    fun `alpha yield fan out shares one cap between reads and observations`() = runTest {
        val release = CompletableDeferred<Unit>()
        var active = 0
        var maximum = 0
        val api = object : BittensorEstimatesApi {
            override suspend fun operations(url: String): Response<BittensorOperationsRemote> = error("Unexpected history request")
            override suspend fun alphaYields(url: String): Response<BittensorAlphaYieldsRemote> {
                active++
                maximum = maxOf(maximum, active)
                release.await()
                active--
                val body = BittensorAlphaYieldsRemote(emptyList(), BittensorAlphaYieldsRemote.Meta(BittensorAlphaYieldsRemote.Components(component)))
                return response(body)
            }
        }
        val base = "https://infra.test/".toHttpUrl()
        val urls = InfrastructureUrls { base }
        val source = createBittensorTestSources(urls, scope = backgroundScope, estimatesApi = api)
        val chain = mock(Chain::class.java)
        val reads = (1..6).map { netuid -> async { source.yields.alphaYields(base, chain, BittensorNetuid.Subnet(netuid)) } }
        val observations = (7..12).map { netuid -> async { source.yields.observeAlphaYields(chain, BittensorNetuid.Subnet(netuid)).first() } }
        val requests = reads + observations
        runCurrent()
        assertEquals(4, maximum)
        release.complete(Unit)
        requests.forEach { it.await() }
        assertEquals(4, maximum)
    }
}
