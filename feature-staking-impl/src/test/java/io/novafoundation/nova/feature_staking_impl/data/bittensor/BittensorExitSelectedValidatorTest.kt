package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitMember
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorExitSelectedValidatorTest {

    private val large = AccountIdKey(ByteArray(32) { 1 })
    private val small = AccountIdKey(ByteArray(32) { 2 })

    private fun member(stake: Long, hold: Long = 0) = BittensorExitMember(BigInteger.valueOf(stake), BigInteger.ZERO, false, BigInteger.valueOf(hold))

    private fun preflight(members: Map<AccountIdKey, BittensorExitMember>, available: Long = members.values.sumOf { it.stake.toLong() }) =
        BittensorExitPreflight(
            stake = members.values.first().stake,
            availability = BittensorExitAvailability(BigInteger.valueOf(available), BigInteger.ZERO, BigInteger.valueOf(available)),
            rootHoldRemaining = BigInteger.ZERO,
            rootHoldEnabled = false,
            minStake = BigInteger.TEN,
            minNominatorStake = BigInteger.TEN,
            isOwnHotkey = false,
            groupStakes = members.mapValues { it.value.stake },
            groupMembers = members
        )

    @Test
    fun `with several validators the stake and max refer to the selected validator`() {
        val preflight = preflight(mapOf(large to member(13_000), small to member(165)))

        assertEquals(BigInteger.valueOf(13_000), preflight.stakeOn(large))
        assertEquals(BigInteger.valueOf(13_000), preflight.maximumAmountOn(large))
        assertEquals(BigInteger.valueOf(13_165), preflight.maximumAmount)
    }

    @Test
    fun `the selected validator max is a single-validator exit, so its fee can come from the stake`() {
        val preflight = preflight(mapOf(large to member(13_000), small to member(165)))

        val plan = preflight.plan(preflight.maximumAmountOn(large), large)

        assertEquals(listOf(large), plan.slices.map { it.hotkey })
        assertEquals(false, plan.isBatched)
    }

    @Test
    fun `a single validator keeps the whole position`() {
        val preflight = preflight(mapOf(large to member(500)))

        assertEquals(BigInteger.valueOf(500), preflight.stakeOn(large))
        assertEquals(preflight.maximumAmount, preflight.maximumAmountOn(large))
    }

    @Test
    fun `a held selected validator offers nothing to unstake and never more than is available`() {
        assertEquals(BigInteger.ZERO, preflight(mapOf(large to member(900, hold = 5), small to member(100))).maximumAmountOn(large))
        assertEquals(BigInteger.valueOf(400), preflight(mapOf(large to member(900), small to member(100)), available = 400).maximumAmountOn(large))
    }
}
