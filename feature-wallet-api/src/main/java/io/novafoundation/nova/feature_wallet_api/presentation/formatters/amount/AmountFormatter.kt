package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.math.BigDecimal

interface AmountFormatter : GenericAmountFormatter<AmountModel>

class RealAmountFormatter(
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter
) : AmountFormatter {

    override fun formatAmountToAmountModel(
        amount: BigDecimal,
        token: TokenBase,
        config: AmountConfig
    ): AmountModel {
        return AmountModel(
            token = tokenFormatter.formatToken(amount, token, config.tokenConfig),
            fiat = formatFiat(token.amountToFiat(amount), token.currency, config)
        )
    }

    private fun formatFiat(
        fiatAmount: BigDecimal,
        currency: Currency,
        config: AmountConfig
    ): CharSequence? {
        if (fiatAmount == BigDecimal.ZERO && !config.includeZeroFiat) return null

        return fiatFormatter.formatFiat(fiatAmount, currency, config.fiatConfig)
    }
}
