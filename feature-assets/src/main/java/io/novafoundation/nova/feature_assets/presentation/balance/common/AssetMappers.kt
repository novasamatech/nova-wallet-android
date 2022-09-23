package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_assets.data.mappers.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.AssetGroupUi
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset

fun GroupedList<AssetGroup, Asset>.mapGroupedAssetsToUi(currency: Currency): List<Any> {
    return mapKeys { (assetGroup, _) -> mapAssetGroupToUi(assetGroup, currency) }
        .mapValues { (_, assets) -> assets.map(::mapAssetToAssetModel) }
        .toListWithHeaders()
}

private fun mapAssetGroupToUi(assetGroup: AssetGroup, currency: Currency) = AssetGroupUi(
    chainUi = mapChainToUi(assetGroup.chain),
    groupBalanceFiat = assetGroup.groupBalanceFiat.formatAsCurrency(currency)
)
