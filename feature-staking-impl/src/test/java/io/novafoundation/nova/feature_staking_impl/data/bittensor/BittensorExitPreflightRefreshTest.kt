package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitMember
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BittensorExitPreflightRefreshTest {

    private val hotkey = AccountIdKey(ByteArray(32) { 1 })

    private fun preflight(stake: Long = 1_000, hold: Long = 0, price: Long = 50) = BittensorExitPreflight(
        stake = BigInteger.valueOf(stake),
        availability = BittensorExitAvailability(BigInteger.valueOf(stake), BigInteger.ZERO, BigInteger.valueOf(stake)),
        rootHoldRemaining = BigInteger.valueOf(hold),
        rootHoldEnabled = true,
        minStake = BigInteger.TEN,
        minNominatorStake = BigInteger.TEN,
        isOwnHotkey = false,
        groupStakes = mapOf(hotkey to BigInteger.valueOf(stake)),
        groupRootHoldRemaining = BigInteger.valueOf(hold),
        groupMembers = mapOf(hotkey to BittensorExitMember(BigInteger.valueOf(stake), BigInteger.ZERO, false, BigInteger.valueOf(hold))),
        withdrawalFloor = BigInteger.valueOf(price / 10),
        withdrawalPrice = BigInteger.valueOf(price)
    )

    @Test
    fun `price drift and hold countdown do not count as a position change`() {
        assertEquals(preflight(price = 50).refreshIdentity(), preflight(price = 51).refreshIdentity())
        assertEquals(preflight(hold = 30).refreshIdentity(), preflight(hold = 29).refreshIdentity())
    }

    @Test
    fun `stake change and hold release are position changes`() {
        assertNotEquals(preflight(stake = 1_000).refreshIdentity(), preflight(stake = 999).refreshIdentity())
        assertNotEquals(preflight(hold = 1).refreshIdentity(), preflight(hold = 0).refreshIdentity())
    }
}
