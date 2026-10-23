package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.feature_staking_impl.data.bittensor.bindBittensorDelegatePermits
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BittensorValidatorCoverageTest {

    @Test
    fun `coverage counts only permits on listed subnets`() {
        val permits = setOf(BittensorNetuid.Subnet(1), BittensorNetuid.Subnet(5), BittensorNetuid.Subnet(99))
        val listed = (1..10).map { BittensorNetuid.Subnet(it) }.toSet()

        assertEquals(BittensorValidatorCoverage(permitted = 2, total = 10), bittensorValidatorCoverage(permits, listed))
    }

    @Test
    fun `a hotkey that is not a delegate has unknown coverage rather than zero`() {
        assertNull(bittensorValidatorCoverage(null, setOf(BittensorNetuid.Subnet(1))))
        assertNull(bindBittensorDelegatePermits(null))
    }

    @Test
    fun `delegate permits drop the root network`() {
        val delegate = Struct.Instance(mapOf("validatorPermits" to listOf(BigInteger.ZERO, BigInteger.ONE, 64.toBigInteger())))

        assertEquals(setOf(BittensorNetuid.Subnet(1), BittensorNetuid.Subnet(64)), bindBittensorDelegatePermits(delegate))
    }
}
