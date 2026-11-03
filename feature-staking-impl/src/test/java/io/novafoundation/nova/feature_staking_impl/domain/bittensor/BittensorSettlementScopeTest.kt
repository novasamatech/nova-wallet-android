package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorSettlementScopeTest {

    @Test
    fun `subnet buys and sells settle only their target network`() {
        val subnet = BittensorNetuid.Subnet(7)
        for (kind in listOf(BittensorOperationKind.STAKE, BittensorOperationKind.UNSTAKE)) {
            val target = BittensorOutcomeTarget(kind, subnet, emptySet())

            assertEquals(BittensorSettlementScope.Network(subnet), target.settlementScope())
        }
    }

    @Test
    fun `root stake changes settle root without marking every subnet`() {
        for (kind in listOf(BittensorOperationKind.STAKE, BittensorOperationKind.UNSTAKE)) {
            val target = BittensorOutcomeTarget(kind, BittensorNetuid.Root, emptySet())

            assertEquals(BittensorSettlementScope.Network(BittensorNetuid.Root), target.settlementScope())
        }
    }

    @Test
    fun `root claims can affect multiple networks and settle the account`() {
        val target = BittensorOutcomeTarget(BittensorOperationKind.CLAIM, BittensorNetuid.Root, emptySet())

        assertEquals(BittensorSettlementScope.Account, target.settlementScope())
    }

    @Test
    fun `an absent target conservatively settles the account`() {
        val target: BittensorOutcomeTarget? = null

        assertEquals(BittensorSettlementScope.Account, target.settlementScope())
    }
}
