package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.common.utils.Fraction.Companion.percents

class SlippageConfig(
    val defaultSlippage: Fraction,
    val slippageTips: List<Fraction>,
    val minAvailableSlippage: Fraction,
    val maxAvailableSlippage: Fraction,
    val smallSlippage: Fraction,
    val bigSlippage: Fraction
) {

    companion object {

        fun default(): SlippageConfig {
            return SlippageConfig(
                defaultSlippage = 0.5.percents,
                slippageTips = listOf(0.1.percents, 0.5.percents, 1.0.percents),
                minAvailableSlippage = 0.01.percents,
                maxAvailableSlippage = 50.0.percents,
                smallSlippage = 0.05.percents,
                bigSlippage = 1.0.percents
            )
        }
    }
}
