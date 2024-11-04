package io.novafoundation.nova.feature_assets.presentation.balance.common.mappers

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
import io.novafoundation.nova.common.utils.formatTokenAmount
import io.novafoundation.nova.common.utils.formatting.formatAsChange
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_account_api.presenatation.chain.getAssetIconOrFallback
import io.novafoundation.nova.feature_assets.domain.common.PricedAmount
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetBalance
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenAssetUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.formatBalanceWithFraction

fun GroupedList<TokenAssetGroup, AssetWithNetwork>.mapGroupedAssetsToUi(
    resourceManager: ResourceManager,
    assetIconProvider: AssetIconProvider,
    assetFilter: (groupId: String, List<TokenAssetUi>) -> List<TokenAssetUi> = { _, assets -> assets },
    groupBalance: (TokenAssetGroup) -> PricedAmount = { it.groupBalance.total },
    balance: (AssetBalance) -> PricedAmount = AssetBalance::total,
): List<BalanceListRvItem> {
    return mapKeys { (group, assets) -> mapTokenAssetGroupToUi(resourceManager, assetIconProvider, group, assets, groupBalance) }
        .mapValues { (group, assets) ->
            val assetModels = mapAssetsToAssetModels(resourceManager, assetIconProvider, group, assets, balance)
            assetFilter(group.itemId, assetModels)
        }
        .toListWithHeaders()
        .filterIsInstance<BalanceListRvItem>()
}

fun mapTokenAssetGroupToUi(
    resourceManager: ResourceManager,
    assetIconProvider: AssetIconProvider,
    assetGroup: TokenAssetGroup,
    assets: List<AssetWithNetwork>,
    groupBalance: (TokenAssetGroup) -> PricedAmount = { it.groupBalance.total }
): TokenGroupUi {
    val balance = groupBalance(assetGroup)
    return TokenGroupUi(
        itemId = assetGroup.groupId,
        tokenIcon = assetIconProvider.getAssetIconOrFallback(assetGroup.token.icon),
        rate = mapCoinRateChange(assetGroup.token.coinRate, assetGroup.token.currency),
        recentRateChange = assetGroup.token.coinRate?.recentRateChange.orZero().formatAsChange(),
        rateChangeColorRes = mapCoinRateChangeColorRes(assetGroup.token.coinRate),
        tokenSymbol = assetGroup.token.symbol.value,
        singleItemGroup = assetGroup.itemsCount <= 1,
        balance = AmountModel(
            token = balance.amount.formatTokenAmount(),
            fiat = balance.fiat.formatAsCurrency(assetGroup.token.currency)
        ).formatBalanceWithFraction(resourceManager, R.dimen.asset_balance_fraction_size),
        groupType = mapType(resourceManager, assetGroup, assets, groupBalance)
    )
}

private fun mapAssetsToAssetModels(
    resourceManager: ResourceManager,
    assetIconProvider: AssetIconProvider,
    group: TokenGroupUi,
    assets: List<AssetWithNetwork>,
    balance: (AssetBalance) -> PricedAmount
): List<TokenAssetUi> {
    return assets.map {
        TokenAssetUi(
            group.getId(),
            mapAssetToAssetModel(resourceManager, it.asset, balance(it.balanceWithOffChain)),
            assetIconProvider.getAssetIconOrFallback(it.asset.token.configuration),
            mapChainToUi(it.chain)
        )
    }
}

private fun mapType(
    resourceManager: ResourceManager,
    assetGroup: TokenAssetGroup,
    assets: List<AssetWithNetwork>,
    groupBalance: (TokenAssetGroup) -> PricedAmount
): TokenGroupUi.GroupType {
    return if (assets.size == 1) {
        val balance = groupBalance(assetGroup)
        TokenGroupUi.GroupType.SingleItem(mapAssetToAssetModel(resourceManager, assets.first().asset, balance))
    } else {
        TokenGroupUi.GroupType.Group
    }
}
