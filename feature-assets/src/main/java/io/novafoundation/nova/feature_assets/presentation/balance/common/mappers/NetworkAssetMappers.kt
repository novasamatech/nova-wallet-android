package io.novafoundation.nova.feature_assets.presentation.balance.common.mappers

import androidx.annotation.ColorRes
import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.utils.formatting.formatAsChange
import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.domain.common.Amount
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.TotalAndTransferableBalance
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.BalanceListRvItem
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.NetworkAssetUi
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.items.NetworkGroupUi
import io.novafoundation.nova.feature_assets.presentation.model.TokenModel
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.CoinRateChange
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import java.math.BigDecimal

fun GroupedList<NetworkAssetGroup, AssetWithOffChainBalance>.mapGroupedAssetsToUi(
    currency: Currency,
    groupBalance: (NetworkAssetGroup) -> BigDecimal = NetworkAssetGroup::groupTotalBalanceFiat,
    balance: (TotalAndTransferableBalance) -> Amount = TotalAndTransferableBalance::total,
): List<BalanceListRvItem> {
    return mapKeys { (assetGroup, _) -> mapAssetGroupToUi(assetGroup, currency, groupBalance) }
        .mapValues { (_, assets) -> mapAssetsToAssetModels(assets, balance) }
        .toListWithHeaders()
        .filterIsInstance<BalanceListRvItem>()
}

fun mapTokenToTokenModel(token: Token): TokenModel {
    return with(token) {
        TokenModel(
            configuration = configuration,
            rate = mapCoinRateChange(token.coinRate, token.currency),
            recentRateChange = (coinRate?.recentRateChange ?: BigDecimal.ZERO).formatAsChange(),
            rateChangeColorRes = mapCoinRateChangeColorRes(coinRate)
        )
    }
}

@ColorRes
fun mapCoinRateChangeColorRes(coinRateChange: CoinRateChange?): Int {
    val rateChange = coinRateChange?.recentRateChange

    return when {
        rateChange == null || rateChange.isZero -> R.color.text_secondary
        rateChange.isNonNegative -> R.color.text_positive
        else -> R.color.text_negative
    }
}

private fun mapAssetsToAssetModels(
    assets: List<AssetWithOffChainBalance>,
    balance: (TotalAndTransferableBalance) -> Amount
): List<BalanceListRvItem> {
    return assets.map { NetworkAssetUi(mapAssetToAssetModel(it.asset, balance(it.balanceWithOffchain))) }
}

private fun mapAssetGroupToUi(
    assetGroup: NetworkAssetGroup,
    currency: Currency,
    groupBalance: (NetworkAssetGroup) -> BigDecimal
): BalanceListRvItem {
    return NetworkGroupUi(
        chainUi = mapChainToUi(assetGroup.chain),
        groupBalanceFiat = groupBalance(assetGroup).formatAsCurrency(currency)
    )
}
