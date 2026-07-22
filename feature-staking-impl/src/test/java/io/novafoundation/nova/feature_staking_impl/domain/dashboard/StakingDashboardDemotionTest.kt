package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.feature_staking_impl.data.dashboard.tier.StakingDashboardTierConfig
import io.novafoundation.nova.runtime.multiNetwork.chain.model.KnownChainIds
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class StakingDashboardDemotionTest {

    @Test
    fun `all 7 demoted chain ids are present in the demotion set`() {
        val expected = setOf(
            KnownChainIds.MANTA_ATLANTIC,
            KnownChainIds.ALEPH_ZERO,
            KnownChainIds.VARA,
            KnownChainIds.ZEITGEIST,
            KnownChainIds.MOONRIVER,
            KnownChainIds.POLKADEX,
            KnownChainIds.TERNOA,
        )
        assertEquals(expected, StakingDashboardTierConfig.demotedChainIds)
    }

    @Test
    fun `manta atlantic chain id hex string is exactly the value from chains json`() {
        assertEquals(
            "f3c7ad88f6a80f366c4be216691411ef0622e8b809b1046ea297ef106058d4eb",
            KnownChainIds.MANTA_ATLANTIC
        )
    }

    @Test
    fun `all 7 chain ids are 64 char hex strings`() {
        StakingDashboardTierConfig.demotedChainIds.forEach { chainId ->
            assertEquals("chainId $chainId should be 64 chars", 64, chainId.length)
            assertTrue("chainId $chainId should be hex", chainId.matches(Regex("[0-9a-f]+")))
        }
    }
}
