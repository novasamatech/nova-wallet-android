package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.BittensorAnalyticsEvent
import io.novafoundation.nova.analytics.SlippageBucket
import io.novafoundation.nova.analytics.SlippageChangeMethod
import io.novafoundation.nova.analytics.SlippageWarning
import io.novafoundation.nova.feature_swap_api.presentation.navigation.SlippageSettingsCommunicator

fun SlippageSettingsCommunicator.Response.bittensorAnalyticsEvent(): BittensorAnalyticsEvent.SlippageChanged {
    val method = when (method) {
        SlippageSettingsCommunicator.Method.PRESET -> SlippageChangeMethod.PRESET
        SlippageSettingsCommunicator.Method.CUSTOM -> SlippageChangeMethod.CUSTOM
        SlippageSettingsCommunicator.Method.RESET -> SlippageChangeMethod.RESET
    }
    val warning = when {
        fraction < 0.001 -> SlippageWarning.MAY_REVERT
        fraction > 0.03 -> SlippageWarning.MAY_FRONTRUN
        else -> SlippageWarning.NONE
    }
    return BittensorAnalyticsEvent.SlippageChanged(method, SlippageBucket.from(fraction * 100), warning)
}
