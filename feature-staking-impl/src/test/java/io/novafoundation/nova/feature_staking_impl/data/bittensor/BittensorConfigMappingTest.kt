package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.common.utils.DecimalFraction
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import java.math.BigDecimal
import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorConfigMappingTest {

    @Test fun `configured commission is included in total spend`() {
        val fee = BittensorSwapFeeRemote.Value(DecimalFraction(BigDecimal("0.005")))
        val remote = BittensorConfigRemote(fee, emptyList())
        val config = mapBittensorConfig(remote)
        assertEquals(BigInteger.valueOf(5_000_000), config.commission.feeFor(BigInteger.valueOf(1_005_000_000)))
    }

    @Test fun `invalid commission uses established fallback`() {
        val fees = listOf(
            null,
            BittensorSwapFeeRemote.Invalid,
            BittensorSwapFeeRemote.Value(DecimalFraction(BigDecimal.ONE)),
            BittensorSwapFeeRemote.Value(DecimalFraction(BigDecimal("-0.1")))
        )
        for (fee in fees) {
            val remote = BittensorConfigRemote(fee, emptyList())
            assertEquals(
                BigDecimal("0.003"),
                mapBittensorConfig(remote).commission.fraction
            )
        }
    }

    @Test fun `valid commission boundaries remain decimal fractions`() {
        for (fraction in listOf(BigDecimal.ZERO, BigDecimal("0.999999999999999999"))) {
            val remote = BittensorConfigRemote(BittensorSwapFeeRemote.Value(DecimalFraction(fraction)), emptyList())

            assertEquals(fraction, mapBittensorConfig(remote).commission.fraction)
        }
    }

    @Test fun `dynamic subnet boundaries are accepted and root or out of range ids are skipped`() {
        val subnetIds = listOf(-1, 0, 1, UShort.MAX_VALUE.toInt(), UShort.MAX_VALUE.toInt() + 1, Int.MAX_VALUE)
        val remote = BittensorConfigRemote(
            BittensorSwapFeeRemote.Value(DecimalFraction(BigDecimal.ZERO)),
            subnetIds.map { BittensorConfigRemote.Subnet(it, "https://example.com/$it.png") }
        )

        assertEquals(
            mapOf(
                BittensorNetuid.Subnet(1) to "https://example.com/1.png",
                BittensorNetuid.Subnet(UShort.MAX_VALUE.toInt()) to "https://example.com/65535.png"
            ),
            mapBittensorConfig(remote).subnetLogos
        )
    }

    @Test fun `logos require valid subnet and secure URL with first entry winning`() {
        val remote = BittensorConfigRemote(
            BittensorSwapFeeRemote.Value(DecimalFraction(BigDecimal.ZERO)),
            listOf(
                BittensorConfigRemote.Subnet(1, "https://example.com/first.png"),
                BittensorConfigRemote.Subnet(1, "https://example.com/second.png"),
                BittensorConfigRemote.Subnet(0, "https://example.com/root.png"),
                BittensorConfigRemote.Subnet(65536, "https://example.com/invalid.png"),
                BittensorConfigRemote.Subnet(2, "http://example.com/insecure.png"),
                null
            )
        )
        val config = mapBittensorConfig(remote)
        assertEquals(mapOf(BittensorNetuid.Subnet(1) to "https://example.com/first.png"), config.subnetLogos)
        assertTrue(config.commission.fraction.compareTo(BigDecimal.ZERO) == 0)
    }
}
