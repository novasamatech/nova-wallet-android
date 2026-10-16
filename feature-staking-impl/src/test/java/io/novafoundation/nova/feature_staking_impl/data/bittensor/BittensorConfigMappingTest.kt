package io.novafoundation.nova.feature_staking_impl.data.bittensor

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class BittensorConfigMappingTest {

    @Test fun `configured commission determines exact service fee`() {
        val remote = BittensorConfigRemote(BigDecimal("0.005"), emptyList())
        val config = mapBittensorConfig(remote)
        assertEquals(BigInteger.valueOf(5_000_000), config.commission.feeFor(BigInteger.valueOf(1_000_000_000)))
    }

    @Test fun `invalid commission uses established fallback`() {
        for (fraction in listOf(null, BigDecimal.ONE, BigDecimal("-0.1"))) {
            val remote = BittensorConfigRemote(fraction, emptyList())
            assertEquals(BigDecimal("0.003"), mapBittensorConfig(remote).commission.fraction)
        }
    }

    @Test fun `logos require valid subnet and secure URL with first entry winning`() {
        val remote = BittensorConfigRemote(
            BigDecimal.ZERO,
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
