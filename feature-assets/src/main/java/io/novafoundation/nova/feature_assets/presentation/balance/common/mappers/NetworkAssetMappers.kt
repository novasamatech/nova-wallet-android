package io.novafoundation.nova.feature_assets.presentation.balance.common.mappers

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
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
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.FiatFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.maskable.MaskableValueFormatter
import java.math.BigDecimal

class NetworkAssetMapperFactory(
    private val fiatFormatter: FiatFormatter,
    private val amountFormatter: AmountFormatter
) {
    fun create(maskableFormatter: MaskableValueFormatter): NetworkAssetMapper {
        return NetworkAssetMapper(maskableFormatter, fiatFormatter, amountFormatter)
    }
}

class NetworkAssetMapper(
    private val maskableFormatter: MaskableValueFormatter,
    private val fiatFormatter: FiatFormatter,
    private val amountFormatter: AmountFormatter
) : CommonAssetMapper(maskableFormatter, amountFormatter) {

    fun mapGroupedAssetsToUi(
        groupedAssets: GroupedList<NetworkAssetGroup, AssetWithOffChainBalance>,
        assetIconProvider: AssetIconProvider,
        currency: Currency,
        groupBalance: (NetworkAssetGroup) -> BigDecimal = NetworkAssetGroup::groupTotalBalanceFiat,
        balance: (AssetBalance) -> PricedAmount = AssetBalance::total,
    ): List<BalanceListRvItem> {
        return groupedAssets.mapKeys { (assetGroup, _) -> mapAssetGroupToUi(assetGroup, currency, groupBalance) }
            .mapValues { (_, assets) -> mapAssetsToAssetModels(assetIconProvider, assets, balance) }
            .toListWithHeaders()
            .filterIsInstance<BalanceListRvItem>()
    }

    private fun mapAssetsToAssetModels(
        assetIconProvider: AssetIconProvider,
        assets: List<AssetWithOffChainBalance>,
        balance: (AssetBalance) -> PricedAmount
    ): List<BalanceListRvItem> {
        return assets.map {
            NetworkAssetUi(
                mapAssetToAssetModel(it.asset, balance(it.balanceWithOffchain)),
                assetIconProvider.getAssetIconOrFallback(it.asset.token.configuration)
            )
        }
    }

    private fun mapAssetGroupToUi(
        assetGroup: NetworkAssetGroup,
        currency: Currency,
        groupBalance: (NetworkAssetGroup) -> BigDecimal
    ): NetworkGroupUi {
        return NetworkGroupUi(
            chainUi = mapChainToUi(assetGroup.chain),
            groupBalanceFiat = maskableFormatter.format { fiatFormatter.formatFiat(groupBalance(assetGroup), currency) }
        )
    }
}
