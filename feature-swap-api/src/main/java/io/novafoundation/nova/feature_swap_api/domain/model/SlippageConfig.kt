package io.novafoundation.nova.feature_swap_api.domain.model

import io.novafoundation.nova.common.utils.Percent

class SlippageConfig(
    val defaultSlippage: Percent,
    val slippageTips: List<Percent>,
    val minAvailableSlippage: Percent,
    val maxAvailableSlippage: Percent,
    val smallSlippage: Percent,
    val bigSlippage: Percent
)
