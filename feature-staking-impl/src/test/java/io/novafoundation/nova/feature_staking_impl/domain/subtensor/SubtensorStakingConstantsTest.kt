package io.novafoundation.nova.feature_staking_impl.domain.subtensor

import io.novafoundation.nova.feature_staking_impl.domain.subtensor.model.SubtensorStakingConstants
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class SubtensorStakingConstantsTest {

    @Test
    fun `fee is 0,3 percent of one TAO`() {
        // 0.3% of 1 TAO (1e9 planks) = 3_000_000
        assertEquals(3_000_000.toBigInteger(), SubtensorStakingConstants.novaFeeAmount(1_000_000_000.toBigInteger()))
    }

    @Test
    fun `dust below threshold floors to zero`() {
        // 333 * 30 / 10_000 = 0.999 -> floors to 0
        assertEquals(BigInteger.ZERO, SubtensorStakingConstants.novaFeeAmount(333.toBigInteger()))
    }

    @Test
    fun `smallest non-zero fee is one plank`() {
        // 334 * 30 / 10_000 = 1.002 -> floors to 1
        assertEquals(BigInteger.ONE, SubtensorStakingConstants.novaFeeAmount(334.toBigInteger()))
    }
}
