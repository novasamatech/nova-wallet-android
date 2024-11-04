package io.novafoundation.nova.feature_assets.presentation.balance.common.mappers

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_assets.domain.common.PricedAmount
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.AssetBalance
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.NetworkAssetUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.NetworkGroupUi
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import java.math.BigDecimal

fun GroupedList<NetworkAssetGroup, AssetWithOffChainBalance>.mapGroupedAssetsToUi(
    resourceManager: ResourceManager,
    assetIconProvider: AssetIconProvider,
    currency: Currency,
    groupBalance: (NetworkAssetGroup) -> BigDecimal = NetworkAssetGroup::groupTotalBalanceFiat,
    balance: (AssetBalance) -> PricedAmount = AssetBalance::total,
): List<BalanceListRvItem> {
    return mapKeys { (assetGroup, _) -> mapAssetGroupToUi(assetGroup, currency, groupBalance) }
        .mapValues { (_, assets) -> mapAssetsToAssetModels(resourceManager, assetIconProvider, assets, balance) }
        .toListWithHeaders()
        .filterIsInstance<BalanceListRvItem>()
}

private fun mapAssetsToAssetModels(
    resourceManager: ResourceManager,
    assetIconProvider: AssetIconProvider,
    assets: List<AssetWithOffChainBalance>,
    balance: (AssetBalance) -> PricedAmount
): List<BalanceListRvItem> {
    return assets.map {
        NetworkAssetUi(
            mapAssetToAssetModel(resourceManager, it.asset, balance(it.balanceWithOffchain)),
            assetIconProvider.getAssetIconOrFallback(it.asset.token.configuration)
        )
    }
}

fun mapAssetGroupToUi(
    assetGroup: NetworkAssetGroup,
    currency: Currency,
    groupBalance: (NetworkAssetGroup) -> BigDecimal
): NetworkGroupUi {
    return NetworkGroupUi(
        chainUi = mapChainToUi(assetGroup.chain),
        groupBalanceFiat = groupBalance(assetGroup).formatAsCurrency(currency)
    )
}
