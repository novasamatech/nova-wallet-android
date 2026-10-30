package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class BittensorSubnetAgeTest {

    @Test
    fun `age is the exact block distance times the block time`() {
        val age = bittensorSubnetAge(BigInteger.valueOf(1_000), BigInteger.valueOf(1_000 + 7_200), 12.seconds)

        assertEquals(1.days, age)
    }

    @Test
    fun `age of a subnet registered at the latest block is zero`() {
        assertEquals(Duration.ZERO, bittensorSubnetAge(BigInteger.TEN, BigInteger.TEN, 12.seconds))
    }

    @Test
    fun `long elapsed time is converted without losing precision`() {
        // Duration keeps millisecond precision up to about 2^62 ms; larger values saturate, so the boundary stays below it.
        val elapsedMillis = (1L shl 61) + 1
        val age = bittensorSubnetAge(BigInteger.ZERO, BigInteger.valueOf(elapsedMillis), 1.milliseconds)

        assertEquals(elapsedMillis, age.inWholeMilliseconds)
    }

    @Test(expected = ArithmeticException::class)
    fun `elapsed time just above Long range fails`() {
        bittensorSubnetAge(BigInteger.ZERO, BigInteger.valueOf(Long.MAX_VALUE).inc(), 1.milliseconds)
    }

    @Test(expected = ArithmeticException::class)
    fun `elapsed time that does not fit a Long fails`() {
        bittensorSubnetAge(BigInteger.ZERO, BigInteger.valueOf(Long.MAX_VALUE), 2.milliseconds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `registration ahead of the chain fails`() {
        bittensorSubnetAge(BigInteger.valueOf(11), BigInteger.TEN, 12.seconds)
    }
}
