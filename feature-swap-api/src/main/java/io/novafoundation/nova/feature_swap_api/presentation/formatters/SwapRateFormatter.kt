package io.novafoundation.nova.feature_swap_api.presentation.formatters

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

interface SwapRateFormatter {

    fun format(rate: BigDecimal, assetIn: Chain.Asset, assetOut: Chain.Asset): String
}
