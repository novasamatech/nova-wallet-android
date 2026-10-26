package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.Struct
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorRuntimePriceSourceTest {

    @Test
    fun `batch decoder reads named netuid and price in protocol units`() {
        val decoded = listOf(row(0, BigInteger.TEN), row(1, BigInteger.valueOf(123)), row(2, BigInteger.ZERO))
        assertEquals(mapOf(BittensorNetuid.Subnet(1) to BigInteger.valueOf(123)), bindBittensorRuntimePrices(decoded))
    }

    @Test
    fun `malformed duplicate and out of range runtime rows are rejected`() {
        for (decoded in listOf(
            listOf(row(1, BigInteger.ONE), row(1, BigInteger.TEN)),
            listOf(row(1, -BigInteger.ONE)),
            listOf(row(1, BigInteger.ONE.shiftLeft(64))),
            listOf(row(65536, BigInteger.ONE)),
            listOf(Struct.Instance(mapOf("netuid" to BigInteger.ONE)))
        )) {
            assertTrue(runCatching { bindBittensorRuntimePrices(decoded) }.isFailure)
        }
    }

    private fun row(netuid: Int, price: BigInteger) = Struct.Instance(mapOf("netuid" to BigInteger.valueOf(netuid.toLong()), "price" to price))
}
