package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorNetuidTest {

    @Test
    fun `zero resolves to the root singleton`() {
        assertSame(BittensorNetuid.Root, BittensorNetuid.from(0))
        assertEquals(0, BittensorNetuid.Root.value)
        assertTrue(BittensorNetuid.Root.isRoot())
    }

    @Test
    fun `nonzero u16 resolves to a subnet`() {
        assertEquals(BittensorNetuid.Subnet(1), BittensorNetuid.from(1))
        assertEquals(BittensorNetuid.Subnet(65535), BittensorNetuid.from(65535))
        assertFalse(BittensorNetuid.Subnet(1).isRoot())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `root cannot be constructed as a subnet`() {
        BittensorNetuid.Subnet(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative IDs are rejected`() {
        BittensorNetuid.from(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `IDs above u16 are rejected`() {
        BittensorNetuid.from(65536)
    }
}
