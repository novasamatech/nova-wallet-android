package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import java.math.BigInteger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorFundingStateTest {

    @Test
    fun `unknown balance does not become zero balance`() {
        assertFalse(bittensorNeedsFunding(null, null, locked = false))
    }

    @Test
    fun `zero balance requires funding before fee loads`() {
        assertTrue(bittensorNeedsFunding(BigInteger.ZERO, null, locked = false))
    }

    @Test
    fun `positive balance with unknown fee keeps amount input`() {
        assertFalse(bittensorNeedsFunding(BigInteger.ONE, null, locked = false))
    }

    @Test
    fun `fee and reserve can exhaust a positive balance`() {
        assertTrue(bittensorNeedsFunding(10_000_001.toBigInteger(), BigInteger.ONE, locked = false))
    }

    @Test
    fun `funding above fee and reserve restores amount input`() {
        assertFalse(bittensorNeedsFunding(10_000_002.toBigInteger(), BigInteger.ONE, locked = false))
    }

    @Test
    fun `locked operation preserves amount input even without funds`() {
        assertFalse(bittensorNeedsFunding(BigInteger.ZERO, BigInteger.ONE, locked = true))
    }
}
