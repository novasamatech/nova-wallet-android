package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.maskable

import io.novafoundation.nova.common.data.model.DiscreetMode
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.toMaskableModel
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.GenericAmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.GenericFiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.math.BigDecimal
import java.math.RoundingMode

interface MaskableAmountFormatter : GenericAmountFormatter<MaskableModel<AmountModel>>, GenericFiatFormatter<MaskableModel<CharSequence>> {
    fun <T> formatAny(valueReceiver: () -> T): MaskableModel<T>
}

class MaskableAmountFormatterFactory(
    private val amountFormatter: AmountFormatter,
    private val fiatFormatter: FiatFormatter
) {
    fun create(discreetMode: DiscreetMode): MaskableAmountFormatter {
        return RealMaskableAmountFormatter(discreetMode, amountFormatter, fiatFormatter)
    }
}

class RealMaskableAmountFormatter(
    private val discreetMode: DiscreetMode,
    private val amountFormatter: AmountFormatter,
    private val fiatFormatter: FiatFormatter
) : MaskableAmountFormatter {

    override fun <T> formatAny(valueReceiver: () -> T): MaskableModel<T> {
        return discreetMode.toMaskableModel(valueReceiver)
    }

    override fun formatAmountToAmountModel(amount: BigDecimal, token: TokenBase, config: AmountConfig): MaskableModel<AmountModel> {
        return formatAny { amountFormatter.formatAmountToAmountModel(amount, token, config) }
    }

    override fun formatFiatNoAbbreviation(amount: BigDecimal, currency: Currency, config: FiatConfig): MaskableModel<CharSequence> {
        return formatAny { fiatFormatter.formatFiatNoAbbreviation(amount, currency, config) }
    }

    override fun formatAsCurrency(amount: BigDecimal, currency: Currency, roundingMode: RoundingMode, config: FiatConfig): MaskableModel<CharSequence> {
        return formatAny { fiatFormatter.formatAsCurrency(amount, currency, roundingMode, config) }
    }

    override fun simpleFormatAsCurrency(amount: BigDecimal, currency: Currency, roundingMode: RoundingMode, config: FiatConfig): MaskableModel<CharSequence> {
        return formatAny { fiatFormatter.simpleFormatAsCurrency(amount, currency, roundingMode, config) }
    }
}
