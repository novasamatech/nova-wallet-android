package io.novafoundation.nova.feature_assets.presentation.balance.common.mappers

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_assets.domain.common.Amount
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel

fun mapCoinRateChange(coinRateChange: CoinRateChange?, currency: Currency): String {
    val rateChange = coinRateChange?.rate
    return rateChange.orZero().formatAsCurrency(currency)
}

fun mapAssetToAssetModel(
    asset: Asset,
    balance: Amount
): AssetModel {
    return AssetModel(
        token = mapTokenToTokenModel(asset.token),
        amount = mapAmountToAmountModel(
            amount = balance.amount,
            asset = asset,
            includeAssetTicker = false
        )
    )
}
