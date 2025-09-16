package io.novafoundation.nova.feature_assets.presentation.balance.common.mappers

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.presentation.getAssetIconOrFallback
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
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.model.AmountConfig
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable.MaskableValueFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.model.FractionPartStyling

class TokenAssetMapperFactory(
    private val amountFormatter: AmountFormatter
) {
    fun create(maskableFormatter: MaskableValueFormatter): TokenAssetMapper {
        return TokenAssetMapper(maskableFormatter, amountFormatter)
    }
}

class TokenAssetMapper(
    private val maskableFormatter: MaskableValueFormatter,
    private val amountFormatter: AmountFormatter
) : CommonAssetMapper(maskableFormatter, amountFormatter) {

    fun mapGroupedAssetsToUi(
        groupedTokens: GroupedList<TokenAssetGroup, AssetWithNetwork>,
        assetIconProvider: AssetIconProvider,
        assetFilter: (groupId: String, List<TokenAssetUi>) -> List<TokenAssetUi> = { _, assets -> assets },
        groupBalance: (TokenAssetGroup) -> PricedAmount = { it.groupBalance.total },
        balance: (AssetBalance) -> PricedAmount = AssetBalance::total,
    ): List<BalanceListRvItem> {
        return groupedTokens.mapKeys { (group, assets) -> mapTokenAssetGroupToUi(assetIconProvider, group, assets, groupBalance) }
            .mapValues { (group, assets) ->
                val assetModels = mapAssetsToAssetModels(assetIconProvider, group, assets, balance)
                assetFilter(group.itemId, assetModels)
            }
            .toListWithHeaders()
            .filterIsInstance<BalanceListRvItem>()
    }

    fun mapTokenAssetGroupToUi(
        assetIconProvider: AssetIconProvider,
        assetGroup: TokenAssetGroup,
        assets: List<AssetWithNetwork>,
        groupBalance: (TokenAssetGroup) -> PricedAmount = { it.groupBalance.total }
    ): TokenGroupUi {
        val balance = groupBalance(assetGroup)
        return TokenGroupUi(
            itemId = assetGroup.groupId,
            tokenIcon = assetIconProvider.getAssetIconOrFallback(assetGroup.tokenInfo.icon),
            rate = mapCoinRateChange(assetGroup.tokenInfo.coinRate, assetGroup.tokenInfo.currency),
            recentRateChange = assetGroup.tokenInfo.coinRate?.recentRateChange.orZero().formatAsChange(),
            rateChangeColorRes = mapCoinRateChangeColorRes(assetGroup.tokenInfo.coinRate),
            tokenSymbol = assetGroup.tokenInfo.symbol.value,
            singleItemGroup = assetGroup.itemsCount <= 1,
            balance = maskableFormatter.format {
                amountFormatter.formatAmountToAmountModel(
                    amount = balance.amount,
                    token = assetGroup.tokenInfo.token,
                    config = AmountConfig(
                        includeAssetTicker = false,
                        tokenFractionPartStyling = FractionPartStyling.Styled(R.dimen.asset_balance_fraction_size)
                    )
                )
            },
            groupType = mapType(assets)
        )
    }

    private fun mapAssetsToAssetModels(
        assetIconProvider: AssetIconProvider,
        group: TokenGroupUi,
        assets: List<AssetWithNetwork>,
        balance: (AssetBalance) -> PricedAmount
    ): List<TokenAssetUi> {
        return assets.map {
            TokenAssetUi(
                group.getId(),
                mapAssetToAssetModel(it.asset, balance(it.balanceWithOffChain)),
                assetIconProvider.getAssetIconOrFallback(it.asset.token.configuration),
                mapChainToUi(it.chain)
            )
        }
    }

    private fun mapType(
        assets: List<AssetWithNetwork>,
    ): TokenGroupUi.GroupType {
        return if (assets.size == 1) {
            TokenGroupUi.GroupType.SingleItem(assets.first().asset.token.configuration)
        } else {
            TokenGroupUi.GroupType.Group
        }
    }
}
