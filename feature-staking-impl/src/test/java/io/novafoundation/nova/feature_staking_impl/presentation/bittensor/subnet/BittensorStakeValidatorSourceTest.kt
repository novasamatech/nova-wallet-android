package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.analytics.StakingValidatorSource
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorStakeValidatorSourceTest {

    @Test
    fun `preset handed over by subnet details is reported as recommended`() {
        assertEquals(StakingValidatorSource.RECOMMENDED, bittensorStakeValidatorSource(false, "preset", presetAddress = "preset"))
    }

    @Test
    fun `a validator the user picked is reported as manual`() {
        assertEquals(StakingValidatorSource.MANUAL, bittensorStakeValidatorSource(false, "picked", presetAddress = "preset"))
        assertEquals(StakingValidatorSource.MANUAL, bittensorStakeValidatorSource(false, "picked", presetAddress = null))
    }

    @Test
    fun `buy more always keeps the existing hotkey`() {
        assertEquals(StakingValidatorSource.EXISTING_HOTKEY, bittensorStakeValidatorSource(true, "held", presetAddress = "held"))
    }
}
