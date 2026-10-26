package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitAvailability
import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorExitPreflight
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorPositionWithdrawalTest {

    private val held = AccountIdKey(ByteArray(32) { 1 })

    private val owner = AccountIdKey(ByteArray(32) { 2 })

    private val preflight = BittensorExitPreflight(
        stake = number(100),
        availability = BittensorExitAvailability(number(200), number(130), number(70)),
        rootHoldRemaining = number(3),
        rootHoldEnabled = true,
        minStake = BigInteger.ZERO,
        minNominatorStake = BigInteger.ZERO,
        isOwnHotkey = false,
        groupStakes = mapOf(held to number(100), owner to number(100)),
        groupMembers = mapOf(
            held to BittensorExitMember(number(100), BigInteger.ZERO, false, number(3)),
            owner to BittensorExitMember(number(100), number(30), true, BigInteger.ZERO)
        )
    )

    @Test
    fun `subnet availability and sell maximum both exclude zero payout positions`() {
        val real = BittensorExitMember(number(1437781835), BigInteger.ZERO, false, BigInteger.ZERO)
        val dust = real.copy(stake = BigInteger.ONE)
        val total = real.stake + dust.stake
        val snapshot = preflight.copy(
            availability = BittensorExitAvailability(total, BigInteger.ZERO, total),
            groupStakes = mapOf(held to real.stake, owner to dust.stake),
            groupMembers = mapOf(held to real, owner to dust),
            withdrawalPrice = number(24121195),
            swapFeeRate = BittensorWithdrawalBounds.DEFAULT_SWAP_FEE_RATE
        )
        val netuid = BittensorNetuid.Subnet(9)
        val result = snapshot.positionWithdrawal(netuid, BigDecimal.ZERO, number(12000))
        val sellMaximum = snapshot.withWithdrawalFloors(netuid, BigDecimal.ZERO).exitGroup(held)
            .maximumFill(BittensorExitSelection.All)

        assertEquals(real.stake, result.available)
        assertEquals(sellMaximum, result.available)
        assertTrue(result.canWithdraw)

        val onlyDust = snapshot.copy(groupMembers = mapOf(owner to dust), groupStakes = mapOf(owner to dust.stake))
            .positionWithdrawal(netuid, BigDecimal.ZERO, number(12000))
        assertEquals(BigInteger.ZERO, onlyDust.available)
        assertFalse(onlyDust.canWithdraw)
    }

    @Test
    fun `hints use each validator hold and owner collateral`() {
        val result = preflight.positionWithdrawal(BittensorNetuid.Root, BigDecimal.ZERO, number(12_000))
        assertEquals(number(70), result.available)
        assertEquals(number(130), result.locked)
        assertEquals(number(36_000), result.holdDurationMillis)
        assertTrue(result.canWithdraw)
    }

    @Test
    fun `all held disables withdrawal without defining add stake eligibility`() {
        val member = BittensorExitMember(number(100), BigInteger.ZERO, false, number(3))
        val result = preflight.copy(groupMembers = mapOf(held to member, owner to member))
            .positionWithdrawal(BittensorNetuid.Root, BigDecimal.ZERO, number(12_000))
        assertEquals(BigInteger.ZERO, result.available)
        assertFalse(result.canWithdraw)
    }

    @Test
    fun `root hold notice uses the earliest unlock and actual network interval`() {
        val members = mapOf(
            held to BittensorExitMember(number(100), BigInteger.ZERO, false, number(3)),
            owner to BittensorExitMember(number(100), BigInteger.ZERO, false, number(8))
        )
        val result = preflight.copy(groupMembers = members, rootHoldInterval = number(20))
            .positionWithdrawal(BittensorNetuid.Root, BigDecimal.ZERO, number(12_000))

        assertEquals(BittensorRootHold(number(36_000), number(240_000)), result.rootHold)
        assertFalse(result.canWithdraw)
    }

    @Test
    fun `unlocked root member hides the group hold notice`() {
        val result = preflight.copy(rootHoldInterval = number(20))
            .positionWithdrawal(BittensorNetuid.Root, BigDecimal.ZERO, number(12_000))

        assertNull(result.rootHold)
        assertTrue(result.canWithdraw)
    }

    @Test
    fun `expired disabled and subnet holds never show a root hold notice`() {
        val member = BittensorExitMember(number(100), BigInteger.ZERO, false, number(3))
        val snapshot = preflight.copy(groupMembers = mapOf(held to member), rootHoldInterval = number(20))
        val blockMillis = number(12_000)

        assertNull(snapshot.copy(rootHoldEnabled = false).positionWithdrawal(BittensorNetuid.Root, BigDecimal.ZERO, blockMillis).rootHold)
        assertNull(snapshot.copy(rootHoldInterval = BigInteger.ZERO).positionWithdrawal(BittensorNetuid.Root, BigDecimal.ZERO, blockMillis).rootHold)
        assertNull(snapshot.positionWithdrawal(BittensorNetuid.Subnet(1), BigDecimal.ZERO, blockMillis).rootHold)
        assertNull(
            snapshot.copy(groupMembers = mapOf(held to member.copy(holdRemaining = BigInteger.ZERO)))
                .positionWithdrawal(BittensorNetuid.Root, BigDecimal.ZERO, blockMillis).rootHold
        )
    }

    @Test
    fun `collateral remainder below withdrawal floor is unavailable`() {
        val result = preflight.copy(withdrawalFloor = number(71))
            .positionWithdrawal(BittensorNetuid.Root, BigDecimal.ZERO, number(12_000))
        assertEquals(BigInteger.ZERO, result.available)
        assertFalse(result.canWithdraw)
    }

    @Test
    fun `runtime safety gates block withdrawal even with drawable stake`() {
        listOf(
            preflight.copy(safeModeActive = true),
            preflight.copy(coldkeySwapPending = true),
            preflight.copy(subnetTradingEnabled = false)
        ).forEach { snapshot ->
            val result = snapshot.positionWithdrawal(BittensorNetuid.Root, BigDecimal.ZERO, number(12_000))
            assertEquals(number(70), result.available)
            assertFalse(result.canWithdraw)
        }
    }

    @Test
    fun `aggregate availability still caps drawable member sum`() {
        val result = preflight.copy(availability = BittensorExitAvailability(number(200), number(150), number(50)))
            .positionWithdrawal(BittensorNetuid.Root, BigDecimal.ZERO, number(12_000))
        assertEquals(number(50), result.available)
        assertEquals(number(150), result.locked)
    }

    private fun number(value: Long) = BigInteger.valueOf(value)
}
