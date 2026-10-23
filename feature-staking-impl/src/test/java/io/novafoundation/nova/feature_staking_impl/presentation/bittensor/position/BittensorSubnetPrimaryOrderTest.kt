package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.position

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetPosition
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorSubnetPrimaryOrderTest {

    private fun position(hotkeyByte: Int, alpha: Long) = BittensorSubnetPosition(
        BittensorNetuid.Subnet(12),
        AccountIdKey(ByteArray(32) { hotkeyByte.toByte() }),
        BigInteger.valueOf(alpha)
    )

    @Test
    fun `largest stake leads regardless of portfolio order`() {
        val small = position(1, 10)
        val large = position(9, 500)

        assertEquals(listOf(large, small), listOf(small, large).inPrimaryOrder())
    }

    @Test
    fun `equal stakes fall back to hotkey order`() {
        val first = position(2, 100)
        val second = position(7, 100)

        assertEquals(listOf(first, second), listOf(second, first).inPrimaryOrder())
    }
}
