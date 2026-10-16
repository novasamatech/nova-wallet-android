package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorMaximumStakeTest {

    @Test
    fun `maximum reserves network fee and the same additional reserve as iOS`() {
        val maximum = maximumBittensorStake(BigInteger.valueOf(100_000_000), BigInteger.valueOf(1_000_000))

        assertEquals(BigInteger.valueOf(89_000_000), maximum)
    }

    @Test
    fun `reserve is retained even when another account pays the network fee`() {
        val maximum = maximumBittensorStake(BigInteger.valueOf(100_000_000), BigInteger.ZERO)

        assertEquals(BigInteger.valueOf(90_000_000), maximum)
    }

    @Test
    fun `fee and reserve consuming the balance leave no stake`() {
        assertEquals(BigInteger.ZERO, maximumBittensorStake(BigInteger.valueOf(11_000_000), BigInteger.valueOf(1_000_000)))
        assertEquals(BigInteger.ZERO, maximumBittensorStake(BigInteger.ONE, BigInteger.TEN))
        assertEquals(BigInteger.ZERO, maximumBittensorStake(BigInteger.ZERO, BigInteger.ZERO))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative network fee is rejected`() {
        maximumBittensorStake(BigInteger.TEN, BigInteger.ONE.negate())
    }
}
