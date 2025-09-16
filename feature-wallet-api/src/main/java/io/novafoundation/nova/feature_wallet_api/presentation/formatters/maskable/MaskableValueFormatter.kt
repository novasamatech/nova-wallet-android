package io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable

import io.novafoundation.nova.common.data.model.MaskingMode
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.toMaskableModel
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.GenericAmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.GenericFiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.GenericTokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.math.BigDecimal

interface MaskableValueFormatter :
    GenericAmountFormatter<MaskableModel<AmountModel>>,
    GenericTokenFormatter<MaskableModel<CharSequence>>,
    GenericFiatFormatter<MaskableModel<CharSequence>> {

    fun <T> formatAny(valueReceiver: () -> T): MaskableModel<T>
}

class MaskableValueFormatterFactory(
    private val amountFormatter: AmountFormatter,
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter
) {
    fun create(maskingMode: MaskingMode): MaskableValueFormatter {
        return RealMaskableValueFormatter(maskingMode, amountFormatter, tokenFormatter, fiatFormatter)
    }
}

class RealMaskableValueFormatter(
    private val maskingMode: MaskingMode,
    private val amountFormatter: AmountFormatter,
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter,
) : MaskableValueFormatter {

    override fun <T> formatAny(valueReceiver: () -> T): MaskableModel<T> {
        return maskingMode.toMaskableModel(valueReceiver)
    }

    override fun formatAmountToAmountModel(amount: BigDecimal, token: TokenBase, config: AmountConfig): MaskableModel<AmountModel> {
        return formatAny { amountFormatter.formatAmountToAmountModel(amount, token, config) }
    }

    override fun formatToken(amount: BigDecimal, token: TokenBase, config: TokenConfig): MaskableModel<CharSequence> {
        return formatAny { tokenFormatter.formatToken(amount, token, config) }
    }

    override fun formatFiat(fiatAmount: BigDecimal, currency: Currency, config: FiatConfig): MaskableModel<CharSequence> {
        return formatAny { fiatFormatter.formatFiat(fiatAmount, currency, config) }
    }
}
