package io.novafoundation.nova.feature_assets.presentation.balance.common

import io.novafoundation.nova.common.list.GroupedList
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_assets.data.mappers.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.presentation.balance.list.model.AssetGroupUi
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_currency_api.domain.model.Currency
import io.novafoundation.nova.feature_currency_api.presentation.formatters.formatAsCurrency
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import java.math.BigInteger

fun GroupedList<AssetGroup, Asset>.mapGroupedAssetsToUi(currency: Currency, offChainBalanceByAssetId: Map<ChainAssetId, BigInteger>): List<Any> {
    return mapKeys { (assetGroup, assets) -> mapAssetGroupToUi(assetGroup, assets, currency, offChainBalanceByAssetId) }
        .mapValues { (_, assets) -> mapAssetsToAssetModels(assets, offChainBalanceByAssetId) }
        .toListWithHeaders()
}

fun mapAssetsToAssetModels(assets: List<Asset>, contributionsByAssetId: Map<ChainAssetId, BigInteger>): List<AssetModel> {
    return assets.map {
        val offChainBalance = contributionsByAssetId[it.token.configuration.id].orZero()
        mapAssetToAssetModel(it, offChainBalance)
    }
}

private fun mapAssetGroupToUi(
    assetGroup: AssetGroup,
    assets: List<Asset>,
    currency: Currency,
    offChainBalanceByAssetId: Map<ChainAssetId, BigInteger>
): AssetGroupUi {
    val offChainGroupFiatBalance = assets.sumOf {
        val offChainInPlanks = offChainBalanceByAssetId[it.token.configuration.id].orZero()
        val offChainTokenAmount = it.token.amountFromPlanks(offChainInPlanks)
        it.token.priceOf(offChainTokenAmount)
    }

    val groupTotalFiat = assetGroup.groupBalanceFiat + offChainGroupFiatBalance
    return AssetGroupUi(
        chainUi = mapChainToUi(assetGroup.chain),
        groupBalanceFiat = groupTotalFiat.formatAsCurrency(currency)
    )
}
