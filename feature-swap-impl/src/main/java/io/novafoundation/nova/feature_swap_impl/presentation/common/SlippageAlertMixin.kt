package io.novafoundation.nova.feature_swap_impl.presentation.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_swap_api.domain.model.SlippageConfig
import io.novafoundation.nova.feature_swap_impl.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class SlippageAlertMixinFactory(
    private val resourceManager: ResourceManager
) {

    fun create(slippageConfig: Flow<SlippageConfig>, slippageFlow: Flow<Fraction?>): SlippageAlertMixin {
        return RealSlippageAlertMixin(
            resourceManager,
            slippageConfig,
            slippageFlow
        )
    }
}

interface SlippageAlertMixin {

    val slippageAlertMessage: Flow<String?>
}

class RealSlippageAlertMixin(
    val resourceManager: ResourceManager,
    slippageConfig: Flow<SlippageConfig>,
    slippageFlow: Flow<Fraction?>
) : SlippageAlertMixin {

    override val slippageAlertMessage: Flow<String?> = combine(slippageConfig, slippageFlow) { slippageConfig, slippage ->
        when {
            slippage == null -> null

            slippage !in slippageConfig.minAvailableSlippage..slippageConfig.maxAvailableSlippage -> null

            slippage > slippageConfig.bigSlippage -> {
                resourceManager.getString(R.string.swap_slippage_warning_too_big)
            }

            slippage < slippageConfig.smallSlippage -> {
                resourceManager.getString(R.string.swap_slippage_warning_too_small)
            }

            else -> null
        }
    }
}
