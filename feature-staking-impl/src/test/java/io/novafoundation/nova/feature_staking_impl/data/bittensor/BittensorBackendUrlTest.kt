package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.infrastructure.InfrastructureUrlMissingException
import io.novafoundation.nova.infrastructure.InfrastructureUrls
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorBackendUrlTest {

    @Test
    fun `rooted route is independent of base path and trailing slash`() {
        for (base in listOf("https://infra.test", "https://infra.test/", "https://infra.test/nested/config")) {
            assertEquals("https://infra.test/v1/bittensor/portfolio/history/search", bittensorBackendUrl(base.toHttpUrl(), "portfolio/history/search"))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `insecure origin is rejected`() {
        bittensorBackendUrl("http://infra.test".toHttpUrl(), "portfolio/history/search")
    }

    @Test(expected = InfrastructureUrlMissingException::class)
    fun `missing origin cannot silently fall back`() = runBlocking<Unit> {
        val urls = InfrastructureUrls { null }
        urls.bittensorBackendUrl("portfolio/history/search")
    }
}
