package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class BittensorMaximumStakeTest {

    @Test
    fun `maximum reserves the network fee`() = runBlocking {
        val maximum = maximumBittensorStake(BigInteger.valueOf(100)) { BigInteger.TEN }
        assertEquals(BigInteger.valueOf(90), maximum)
    }

    @Test
    fun `fee is checked again for the reduced amount`() = runBlocking {
        val amounts = mutableListOf<BigInteger>()
        val maximum = maximumBittensorStake(BigInteger.valueOf(100)) {
            amounts += it
            if (it == BigInteger.valueOf(100)) BigInteger.TEN else BigInteger.valueOf(15)
        }
        assertEquals(BigInteger.valueOf(85), maximum)
        assertEquals(listOf(100L, 90L, 85L).map(BigInteger::valueOf), amounts)
    }

    @Test
    fun `fee larger than balance leaves no stake`() = runBlocking {
        assertEquals(BigInteger.ZERO, maximumBittensorStake(BigInteger.ONE) { BigInteger.TEN })
    }
}
