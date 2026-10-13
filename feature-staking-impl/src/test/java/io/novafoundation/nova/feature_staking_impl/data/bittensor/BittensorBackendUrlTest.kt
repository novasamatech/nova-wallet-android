package io.novafoundation.nova.feature_staking_impl.data.bittensor

import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorBackendUrlTest {

    @Test
    fun `resolves path under the v1 bittensor root`() {
        assertEquals(
            "https://api.example/v1/bittensor/portfolio/history/search",
            bittensorBackendUrl("https://api.example", "portfolio/history/search")
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects plain http`() {
        bittensorBackendUrl("http://api.example", "subnets")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects unconfigured host`() {
        bittensorBackendUrl("", "subnets")
    }
}
