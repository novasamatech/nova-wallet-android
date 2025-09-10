package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.maskable

import io.novafoundation.nova.common.data.model.DiscreetMode
import io.novafoundation.nova.common.presentation.model.MaskableModel
import io.novafoundation.nova.common.presentation.model.toMaskableModel
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.GenericAmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.math.BigDecimal

interface MaskableAmountFormatter : GenericAmountFormatter<MaskableModel<AmountModel>>

class MaskableAmountFormatterFactory(private val amountFormatter: AmountFormatter) {
    fun create(discreetMode: DiscreetMode): MaskableAmountFormatter {
        return RealMaskableAmountFormatter(discreetMode, amountFormatter)
    }
}

class RealMaskableAmountFormatter(
    private val discreetMode: DiscreetMode,
    private val amountFormatter: AmountFormatter
) : MaskableAmountFormatter {
    override fun formatAmountToAmountModel(amount: BigDecimal, token: TokenBase, config: AmountConfig): MaskableModel<AmountModel> {
        return discreetMode.toMaskableModel {
            amountFormatter.formatAmountToAmountModel(amount, token, config)
        }
    }
}
