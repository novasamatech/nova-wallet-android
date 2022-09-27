package io.novafoundation.nova.feature_assets.data.mappers.mappers

import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.common.utils.formatting.formatAsChange
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_assets.presentation.model.TokenModel
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import java.math.BigDecimal
import java.math.BigInteger

fun mapTokenToTokenModel(token: Token): TokenModel {
    return with(token) {
        val rateChange = token.recentRateChange

        val changeColorRes = when {
            rateChange == null -> R.color.gray2
            rateChange.isNonNegative -> R.color.green
            else -> R.color.red
        }

        TokenModel(
            configuration = configuration,
            rate = (rate ?: BigDecimal.ZERO).formatAsCurrency(token.currency),
            recentRateChange = (recentRateChange ?: BigDecimal.ZERO).formatAsChange(),
            rateChangeColorRes = changeColorRes
        )
    }
}

fun mapAssetToAssetModel(asset: Asset, offChainBalance: BigInteger): AssetModel {
    return with(asset) {
        val offChainAmount = asset.token.amountFromPlanks(offChainBalance)
        val assetTotalFiat = priceAmount + token.priceOf(offChainAmount)
        AssetModel(
            token = mapTokenToTokenModel(token),
            total = total.format(),
            priceAmount = assetTotalFiat.formatAsCurrency(asset.token.currency)
        )
    }
}
