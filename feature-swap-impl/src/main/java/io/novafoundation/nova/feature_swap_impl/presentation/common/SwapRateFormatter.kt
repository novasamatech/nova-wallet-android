package io.novafoundation.nova.feature_swap_impl.presentation.common

import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

interface SwapRateFormatter {

    fun format(rate: BigDecimal, assetIn: Chain.Asset, assetOut: Chain.Asset): String
}

class RealSwapRateFormatter : SwapRateFormatter {

    override fun format(rate: BigDecimal, assetIn: Chain.Asset, assetOut: Chain.Asset): String {
        val assetInUnitFormatted = BigDecimal.ONE.formatTokenAmount(assetIn)
        val rateAmountFormatted = rate.formatTokenAmount(assetOut)

        return "$assetInUnitFormatted ≈ $rateAmountFormatted"
    }
}
