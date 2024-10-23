package io.novafoundation.nova.feature_assets.presentation.balance.common.mappers

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.utils.formatTokenAmount
import io.novafoundation.nova.common.utils.formatting.formatAsChange
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_assets.domain.common.Amount
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.TotalAndTransferableBalance
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenAssetUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.TokenGroupUi
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

fun GroupedList<TokenAssetGroup, AssetWithNetwork>.mapGroupedAssetsToUi(
    assetFilter: (groupId: String, List<TokenAssetUi>) -> List<TokenAssetUi>,
    groupBalance: (TokenAssetGroup) -> Amount = { it.groupBalance.total },
    balance: (TotalAndTransferableBalance) -> Amount = TotalAndTransferableBalance::total,
): List<BalanceListRvItem> {
    return mapKeys { (group, assets) -> mapAssetGroupToUi(group, assets, groupBalance) }
        .mapValues { (group, assets) ->
            val assetModels = mapAssetsToAssetModels(group, assets, balance)
            assetFilter(group.itemId, assetModels)
        }
        .toListWithHeaders()
        .filterIsInstance<BalanceListRvItem>()
}

private fun mapAssetsToAssetModels(
    group: TokenGroupUi,
    assets: List<AssetWithNetwork>,
    balance: (TotalAndTransferableBalance) -> Amount
): List<TokenAssetUi> {
    return assets.map { TokenAssetUi(group.getId(), mapAssetToAssetModel(it.asset, balance(it.balanceWithOffChain)), mapChainToUi(it.chain)) }
}

private fun mapAssetGroupToUi(
    assetGroup: TokenAssetGroup,
    assets: List<AssetWithNetwork>,
    groupBalance: (TokenAssetGroup) -> Amount
): TokenGroupUi {
    val balance = groupBalance(assetGroup)
    return TokenGroupUi(
        itemId = assetGroup.groupId,
        tokenIcon = assetGroup.token.icon,
        rate = mapCoinRateChange(assetGroup.token.coinRate, assetGroup.token.currency),
        recentRateChange = assetGroup.token.coinRate?.recentRateChange.orZero().formatAsChange(),
        rateChangeColorRes = mapCoinRateChangeColorRes(assetGroup.token.coinRate),
        tokenSymbol = assetGroup.token.symbol.value,
        groupWithOneItem = assetGroup.itemsCount == 1,
        balance = AmountModel(
            token = balance.amount.formatTokenAmount(),
            fiat = balance.fiat.formatAsCurrency(assetGroup.token.currency)
        ),
        groupType = mapType(assetGroup, assets, groupBalance)
    )
}

private fun mapType(
    assetGroup: TokenAssetGroup,
    assets: List<AssetWithNetwork>,
    groupBalance: (TokenAssetGroup) -> Amount
): TokenGroupUi.GroupType {
    return if (assets.size == 1) {
        val balance = groupBalance(assetGroup)
        TokenGroupUi.GroupType.SingleItem(mapAssetToAssetModel(assets.first().asset, balance))
    } else {
        TokenGroupUi.GroupType.Group
    }
}
