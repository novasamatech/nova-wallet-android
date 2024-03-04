package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Percent

class SlippageConfig(
    val defaultSlippage: Percent,
    val slippageTips: List<Percent>,
    val minAvailableSlippage: Percent,
    val maxAvailableSlippage: Percent,
    val smallSlippage: Percent,
    val bigSlippage: Percent
) {

    companion object {

        fun default(): SlippageConfig {
            return SlippageConfig(
                defaultSlippage = Percent(0.5),
                slippageTips = listOf(Percent(0.1), Percent(0.5), Percent(1.0)),
                minAvailableSlippage = Percent(0.01),
                maxAvailableSlippage = Percent(50.0),
                smallSlippage = Percent(0.05),
                bigSlippage = Percent(1.0)
            )
        }
    }
}
