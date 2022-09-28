package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_assets.data.mappers.mappers.mapTokenToTokenModel
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.AssetGroupUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel

fun GroupedList<AssetGroup, AssetWithOffChainBalance>.mapGroupedAssetsToUi(
    currency: Currency,
): List<Any> {
    return mapKeys { (assetGroup, _) -> mapAssetGroupToUi(assetGroup, currency) }
        .mapValues { (_, assets) -> mapAssetsToAssetModels(assets) }
        .toListWithHeaders()
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
