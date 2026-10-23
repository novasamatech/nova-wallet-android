package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import io.novafoundation.nova.common.address.AccountIdKey
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
}
