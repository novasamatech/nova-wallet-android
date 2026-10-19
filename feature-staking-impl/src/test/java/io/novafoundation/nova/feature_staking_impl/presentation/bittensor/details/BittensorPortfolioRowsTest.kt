package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.details

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorPortfolioPositions
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPortfolioRowsTest {

    private val empty = BittensorPortfolioPositions(emptyList(), emptyList(), emptyMap())

    private val hotkey = AccountIdKey(ByteArray(32))

    @Test
    fun `rewards only root is visible and not empty without adding rewards to stake`() {
        assertTrue(hasRootPortfolioRow(empty, BigInteger.ONE))
        assertFalse(isPortfolioEmpty(empty, BigInteger.ONE))
        assertEquals(BigInteger.ZERO, empty.totalStake)
        assertTrue(isPortfolioEmpty(empty, BigInteger.ZERO))
        assertFalse(isPortfolioEmpty(empty, null))
        assertFalse(isPortfolioEmpty(null, BigInteger.ZERO))
    }

    @Test
    fun `root subnet and mixed stake remain visible while rewards are unknown`() {
        val root = BittensorRootPosition(hotkey, BigInteger.TEN)
        val subnet = BittensorSubnetPosition(BittensorNetuid.Subnet(1), hotkey, BigInteger.TEN)
        assertTrue(hasRootPortfolioRow(empty.copy(root = listOf(root)), null))
        assertFalse(isPortfolioEmpty(empty.copy(subnets = listOf(subnet)), BigInteger.ZERO))
        assertFalse(isPortfolioEmpty(empty.copy(root = listOf(root), subnets = listOf(subnet)), null))
    }

    @Test
    fun `active uses registration of largest position with hotkey tie break`() {
        val first = BittensorRootPosition(hotkey, BigInteger.TEN, false)
        val second = BittensorRootPosition(AccountIdKey(ByteArray(32) { 1 }), BigInteger.ONE, true)
        assertEquals(false, listOf(second, first).primaryRootRegistration())
        assertEquals(true, listOf(first, second.copy(stakeInPlanks = BigInteger.valueOf(11))).primaryRootRegistration())
        assertEquals(false, listOf(second.copy(stakeInPlanks = BigInteger.TEN), first).primaryRootRegistration())
        assertNull(listOf(first.copy(isRegistered = null)).primaryRootRegistration())
        assertNull(emptyList<BittensorRootPosition>().primaryRootRegistration())
    }
}
