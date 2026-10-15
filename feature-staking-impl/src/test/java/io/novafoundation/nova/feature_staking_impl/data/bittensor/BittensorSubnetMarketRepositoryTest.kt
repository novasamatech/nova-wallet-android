package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorSubnetMarketRepositoryTest {

    @Test
    fun `market ids use netuid symbols rather than names or alpha symbols`() {
        val rows = listOf(
            BittensorSubnetMarketRemote("vanta", " SN8 "),
            BittensorSubnetMarketRemote("wrong", "theta"),
            BittensorSubnetMarketRemote("root", "sn0"),
            BittensorSubnetMarketRemote("overflow", "sn65536"),
            BittensorSubnetMarketRemote("invalid/id", "sn9"),
            BittensorSubnetMarketRemote("duplicate", "sn8"),
            BittensorSubnetMarketRemote(null, "sn10")
        )

        assertEquals(mapOf(BittensorNetuid.Subnet(8) to "vanta"), bindBittensorMarketIds(rows))
    }
}
