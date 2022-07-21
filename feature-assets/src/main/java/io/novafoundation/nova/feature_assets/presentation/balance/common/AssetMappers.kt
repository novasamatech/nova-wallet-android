package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_assets.data.mappers.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.AssetGroupUi
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun Flow<GroupedList<AssetGroup, Asset>>.mapGroupedAssetsToUi() = map { assets ->
    assets
        .mapKeys { (assetGroup, _) -> mapAssetGroupToUi(assetGroup) }
        .mapValues { (_, assets) -> assets.map(::mapAssetToAssetModel) }
        .toListWithHeaders()
}

private fun mapAssetGroupToUi(assetGroup: AssetGroup) = AssetGroupUi(
    chainUi = mapChainToUi(assetGroup.chain),
    groupBalanceFiat = assetGroup.groupBalanceFiat.formatAsCurrency()
)
