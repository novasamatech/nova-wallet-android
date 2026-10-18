package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class BittensorStakeAmountPresetTest {

    @Test
    fun `fixed presets do not depend on the available balance`() {
        val balance = BigDecimal("0.25")

        assertEquals(BigDecimal.ONE, BittensorStakeAmountPreset.ONE.amountFor(balance))
        assertEquals(BigDecimal("5"), BittensorStakeAmountPreset.FIVE.amountFor(balance))
        assertEquals(BigDecimal.TEN, BittensorStakeAmountPreset.TEN.amountFor(balance))
    }

    @Test
    fun `max resolves to the current balance including zero`() {
        listOf(BigDecimal("0.03918"), BigDecimal("2.5"), BigDecimal.ZERO).forEach { balance ->
            assertEquals(balance, BittensorStakeAmountPreset.MAX.amountFor(balance))
        }
    }
}
