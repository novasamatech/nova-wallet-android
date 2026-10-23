package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorDataStatus
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorRootPosition
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorRootStakeChangeTest {

    private val first = AccountIdKey(ByteArray(32) { 1 })
    private val second = AccountIdKey(ByteArray(32) { 2 })

    @Test
    fun `adding to the only validator keeps a single validator and sums the stake`() {
        val change = bittensorRootStakeChange(listOf(BittensorRootPosition(first, BigInteger("100"))), first, BigInteger("50"))

        assertEquals(BigInteger("100"), change.before)
        assertEquals(BigInteger("150"), change.after)
        assertEquals(1, change.validatorCount)
    }

    @Test
    fun `the whole root stake counts every validator with a positive stake`() {
        val positions = listOf(
            BittensorRootPosition(first, BigInteger("100")),
            BittensorRootPosition(second, BigInteger("30")),
            BittensorRootPosition(AccountIdKey(ByteArray(32) { 3 }), BigInteger.ZERO)
        )

        val change = bittensorRootStakeChange(positions, first, BigInteger("20"))

        assertEquals(BigInteger("130"), change.before)
        assertEquals(BigInteger("150"), change.after)
        assertEquals(2, change.validatorCount)
    }

    @Test
    fun `a new validator is counted after the operation`() {
        val change = bittensorRootStakeChange(listOf(BittensorRootPosition(first, BigInteger("100"))), second, BigInteger("10"))

        assertEquals(2, change.validatorCount)
    }

    private val resources = mock(ResourceManager::class.java)

    private val fee = BittensorFeeUi("0.001 TAO", null)

    @Test
    fun `stake after shows only for add stake with fresh positions`() {
        assertTrue(bittensorShowsRootStakeAfter(true, BittensorDataStatus.Fresh))
        assertFalse(bittensorShowsRootStakeAfter(false, BittensorDataStatus.Fresh))
        assertFalse(bittensorShowsRootStakeAfter(true, BittensorDataStatus.Loading))
        assertFalse(bittensorShowsRootStakeAfter(true, BittensorDataStatus.Stale(RuntimeException())))
    }

    @Test
    fun `a new position lists the validator and offers subnet discovery`() {
        `when`(resources.getString(R.string.bittensor_staking_success_title, "0.02 TAO")).thenReturn("You staked 0.02 TAO to root")
        `when`(resources.getString(R.string.bittensor_staking_success_description)).thenReturn("live")

        val success = bittensorRootStakeSuccess(resources, false, "0.02 TAO", "0.02 TAO", "1T1B.AI", 1, fee, ACTIONS)

        assertEquals("You staked 0.02 TAO to root", success.title)
        assertEquals("1T1B.AI", success.validator)
        assertNull(success.stakeAfter)
        assertNull(success.amount)
        assertEquals(ACTIONS, success.actions)
    }

    @Test
    fun `adding stake to a single validator names it`() {
        `when`(resources.getString(R.string.bittensor_staking_more_success_title, "0.01 TAO")).thenReturn("more")
        `when`(resources.getString(R.string.bittensor_staking_more_success_description_with, "0.03 TAO", "1T1B.AI")).thenReturn("with")

        val success = bittensorRootStakeSuccess(resources, true, "0.01 TAO", "0.03 TAO", "1T1B.AI", 1, fee, ACTIONS)

        assertEquals("more", success.title)
        assertEquals("with", success.description)
        assertNull(success.validator)
        assertEquals("0.03 TAO", success.stakeAfter)
        assertEquals(ACTIONS, success.actions)
    }

    @Test
    fun `adding stake with several validators reports only the total`() {
        `when`(resources.getString(R.string.bittensor_staking_more_success_title, "0.01 TAO")).thenReturn("more")
        `when`(resources.getString(R.string.bittensor_staking_more_success_description, "0.03 TAO")).thenReturn("total")

        val success = bittensorRootStakeSuccess(resources, true, "0.01 TAO", "0.03 TAO", "1T1B.AI", 2, fee, ACTIONS)

        assertEquals("total", success.description)
    }
}

private val ACTIONS = listOf(
    io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction.DISCOVER_SUBNETS,
    io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction.VIEW_POSITION
)
