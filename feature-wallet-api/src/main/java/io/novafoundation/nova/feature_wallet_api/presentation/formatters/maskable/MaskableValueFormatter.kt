package io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable

import io.novafoundation.nova.common.data.model.MaskingMode
import io.novafoundation.nova.common.presentation.masking.MaskableModel
import io.novafoundation.nova.common.presentation.masking.toMaskableModel
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.TokenFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.FiatConfig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.TokenConfig
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.math.BigDecimal

interface MaskableValueFormatter {

    fun <T> format(valueReceiver: () -> T): MaskableModel<T>
}

class MaskableValueFormatterFactory {
    fun create(maskingMode: MaskingMode): MaskableValueFormatter {
        return RealMaskableValueFormatter(maskingMode)
    }
}

private class RealMaskableValueFormatter(private val maskingMode: MaskingMode) : MaskableValueFormatter {

    override fun <T> format(valueReceiver: () -> T): MaskableModel<T> {
        return maskingMode.toMaskableModel(valueReceiver)
    }
}
