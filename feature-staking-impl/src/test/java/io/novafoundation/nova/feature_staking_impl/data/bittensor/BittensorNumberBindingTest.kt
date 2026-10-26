package io.novafoundation.nova.feature_staking_impl.data.bittensor

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorNumberBindingTest {

    @Test
    fun `u16 accepts boundary values`() {
        assertEquals(0, bindBittensorU16(BigInteger.ZERO))
        assertEquals(65535, bindBittensorU16(BigInteger.valueOf(65535)))
    }

    @Test
    fun `u16 rejects negative and overflowing values before narrowing`() {
        listOf(BigInteger.valueOf(-1), BigInteger.valueOf(65536), BigInteger.ONE.shiftLeft(32)).forEach {
            assertTrue(runCatching { bindBittensorU16(it) }.exceptionOrNull() is IllegalArgumentException)
        }
    }

    @Test
    fun `u8 accepts boundary values`() {
        assertEquals(0, bindBittensorU8(BigInteger.ZERO))
        assertEquals(255, bindBittensorU8(BigInteger.valueOf(255)))
    }

    @Test
    fun `u8 rejects negative and overflowing values before narrowing`() {
        listOf(BigInteger.valueOf(-1), BigInteger.valueOf(256), BigInteger.ONE.shiftLeft(32)).forEach {
            assertTrue(runCatching { bindBittensorU8(it) }.exceptionOrNull() is IllegalArgumentException)
        }
    }
}
