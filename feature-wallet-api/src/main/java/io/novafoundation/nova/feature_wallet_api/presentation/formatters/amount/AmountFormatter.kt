package io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.PricedAmount
import io.novafoundation.nova.feature_wallet_api.domain.model.TokenBase
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import java.math.BigDecimal
import java.math.BigInteger

interface AmountFormatter {

    fun formatAmountToAmountModel(
        pricedAmount: PricedAmount,
        token: TokenSymbol,
        config: AmountConfig = AmountConfig()
    ): AmountModel
}

class RealAmountFormatter(
    private val tokenFormatter: TokenFormatter,
    private val fiatFormatter: FiatFormatter
) : AmountFormatter {

    override fun formatAmountToAmountModel(
        pricedAmount: PricedAmount,
        token: TokenSymbol,
        config: AmountConfig
    ): AmountModel {
        return AmountModel(
            token = tokenFormatter.formatToken(pricedAmount.amount, token, config.tokenConfig),
            fiat = formatFiat(pricedAmount.price, pricedAmount.currency, config)
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

fun AmountFormatter.formatAmountToAmountModel(
    amountInPlanks: BigInteger,
    asset: Asset,
    config: AmountConfig = AmountConfig()
): AmountModel {
    val amount = asset.token.amountFromPlanks(amountInPlanks)
    return formatAmountToAmountModel(
        pricedAmount = PricedAmount(
            amount = amount,
            price = asset.token.amountToFiat(amount),
            currency = asset.token.currency
        ),
        token = asset.token.configuration.symbol,
        config = config
    )
}

fun AmountFormatter.formatAmountToAmountModel(
    amountInPlanks: BigInteger,
    token: TokenBase,
    config: AmountConfig = AmountConfig()
): AmountModel {
    val amount = token.amountFromPlanks(amountInPlanks)
    return formatAmountToAmountModel(
        pricedAmount = PricedAmount(
            amount = amount,
            price = token.amountToFiat(amount),
            currency = token.currency
        ),
        token = token.configuration.symbol,
        config = config
    )
}

fun AmountFormatter.formatAmountToAmountModel(
    amount: BigDecimal,
    asset: Asset,
    config: AmountConfig = AmountConfig()
) = formatAmountToAmountModel(
    pricedAmount = PricedAmount(
        amount = amount,
        price = asset.token.amountToFiat(amount),
        currency = asset.token.currency
    ),
    token = asset.token.configuration.symbol,
    config = config
)

fun AmountFormatter.formatAmountToAmountModel(
    amount: BigDecimal,
    token: TokenBase,
    config: AmountConfig = AmountConfig()
) = formatAmountToAmountModel(
    pricedAmount = PricedAmount(
        amount = amount,
        price = token.amountToFiat(amount),
        currency = token.currency
    ),
    token = token.configuration.symbol,
    config = config
)

fun Asset.transferableAmountModel(amountFormatter: AmountFormatter) = amountFormatter.formatAmountToAmountModel(transferable, this)

fun transferableAmountModelOf(amountFormatter: AmountFormatter, asset: Asset) =
    amountFormatter.formatAmountToAmountModel(asset.transferable, asset)
