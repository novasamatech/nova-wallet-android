package io.novafoundation.nova.feature_assets.presentation.balance.common.mappers

import androidx.annotation.ColorRes
import io.novafoundation.nova.common.utils.formatting.formatAsChange
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.common.PricedAmount
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_assets.presentation.model.TokenModel
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatBalanceWithFraction
import java.math.BigDecimal

fun mapCoinRateChange(coinRateChange: CoinRateChange?, currency: Currency): String {
    val rateChange = coinRateChange?.rate
    return mapCoinRateChange(rateChange.orZero(), currency)
}

fun mapCoinRateChange(rate: BigDecimal, currency: Currency): String {
    return rate.formatAsCurrency(currency)
}

fun mapAssetToAssetModel(
    amountFormatter: AmountFormatter,
    asset: Asset,
    balance: PricedAmount
): AssetModel {
    return AssetModel(
        token = mapTokenToTokenModel(asset.token),
        amount = amountFormatter.formatAmountToAmountModel(
            amount = balance.amount,
            token = asset.token,
            config = AmountConfig(includeAssetTicker = false)
        ).formatBalanceWithFraction(amountFormatter, R.dimen.asset_balance_fraction_size)
    )
}

@ColorRes
fun mapCoinRateChangeColorRes(coinRateChange: CoinRateChange?): Int {
    val rateChange = coinRateChange?.recentRateChange

    return when {
        rateChange == null || rateChange.isZero -> R.color.text_secondary
        rateChange.isNonNegative -> R.color.text_positive
        else -> R.color.text_negative
    }
}

fun mapTokenToTokenModel(token: Token): TokenModel {
    return with(token) {
        TokenModel(
            configuration = configuration,
            rate = mapCoinRateChange(token.coinRate, token.currency),
            recentRateChange = (coinRate?.recentRateChange ?: BigDecimal.ZERO).formatAsChange(),
            rateChangeColorRes = mapCoinRateChangeColorRes(coinRate)
        )
    }
}
