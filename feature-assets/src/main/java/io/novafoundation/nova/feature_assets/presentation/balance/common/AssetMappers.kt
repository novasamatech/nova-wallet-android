package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.utils.formatting.formatAsChange
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.AssetGroupUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_assets.presentation.model.TokenModel
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import java.math.BigDecimal

fun GroupedList<AssetGroup, AssetWithOffChainBalance>.mapGroupedAssetsToUi(
    currency: Currency,
): List<Any> {
    return mapKeys { (assetGroup, _) -> mapAssetGroupToUi(assetGroup, currency) }
        .mapValues { (_, assets) -> mapAssetsToAssetModels(assets) }
        .toListWithHeaders()
}

fun mapTokenToTokenModel(token: Token): TokenModel {
    return with(token) {
        val rateChange = token.coinRateChange?.recentRateChange

        val changeColorRes = when {
            rateChange == null || rateChange.isZero -> R.color.text_secondary
            rateChange.isNonNegative -> R.color.text_positive
            else -> R.color.text_negative
        }

        TokenModel(
            configuration = configuration,
            rate = coinRateChange?.rate.orZero().formatAsCurrency(token.currency),
            recentRateChange = (coinRateChange?.recentRateChange ?: BigDecimal.ZERO).formatAsChange(),
            rateChangeColorRes = changeColorRes
        )
    }
}

private fun mapAssetsToAssetModels(
    assets: List<AssetWithOffChainBalance>,
): List<AssetModel> {
    return assets.map(::mapAssetToAssetModel)
}

private fun mapAssetGroupToUi(
    assetGroup: AssetGroup,
    currency: Currency,
): AssetGroupUi {
    return AssetGroupUi(
        chainUi = mapChainToUi(assetGroup.chain),
        groupBalanceFiat = assetGroup.groupBalanceFiat.formatAsCurrency(currency)
    )
}

private fun mapAssetToAssetModel(assetWithOffChainBalance: AssetWithOffChainBalance): AssetModel {
    return with(assetWithOffChainBalance) {
        AssetModel(
            token = mapTokenToTokenModel(asset.token),
            amount = mapAmountToAmountModel(
                amount = totalWithOffChain.amount,
                asset = asset,
                includeAssetTicker = false
            )
        )
    }
}
