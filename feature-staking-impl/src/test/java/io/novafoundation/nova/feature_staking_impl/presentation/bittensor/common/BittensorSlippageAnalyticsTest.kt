package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator
import org.junit.Assert.assertEquals
import org.junit.Test

class BittensorSlippageAnalyticsTest {
    @Test
    fun `slippage preserves selection method and uses percentage buckets`() {
        val response = SlippageSettingsCommunicator.Response(0.005, SlippageSettingsCommunicator.Method.RESET)
        val event = response.bittensorAnalyticsEvent()
        assertEquals("reset", event.properties["method"])
        assertEquals("low", event.properties["slippage_bucket"])
        assertEquals("none", event.properties["warning"])
    }

    @Test
    fun `warning thresholds match the settings screen`() {
        assertEquals("may_revert", SlippageSettingsCommunicator.Response(0.0009).bittensorAnalyticsEvent().properties["warning"])
        assertEquals("none", SlippageSettingsCommunicator.Response(0.001).bittensorAnalyticsEvent().properties["warning"])
        assertEquals("none", SlippageSettingsCommunicator.Response(0.03).bittensorAnalyticsEvent().properties["warning"])
        assertEquals("may_frontrun", SlippageSettingsCommunicator.Response(0.031).bittensorAnalyticsEvent().properties["warning"])
    }
}
