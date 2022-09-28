package io.novafoundation.nova.feature_assets.domain.common

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.sumByBigDecimal
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigDecimal
import java.math.BigInteger

class AssetGroup(
    val chain: Chain,
    val groupBalanceFiat: BigDecimal,
    val zeroBalance: Boolean
)

class AssetWithOffChainBalance(
    val asset: Asset,
    val totalWithOffChain: TotalBalance,
) {

    class TotalBalance(
        val amount: BigDecimal,
        val fiat: BigDecimal
    )
}

fun groupAndSortAssetsByNetwork(
    assets: List<Asset>,
    offChainBalancesByFullAssetId: Map<FullChainAssetId, BigInteger>,
    chainsById: Map<String, Chain>
): Map<AssetGroup, List<AssetWithOffChainBalance>> {
    val assetGroupComparator = compareByDescending(AssetGroup::groupBalanceFiat)
        .thenByDescending { it.zeroBalance } // non-zero balances first
        .then(Chain.defaultComparatorFrom(AssetGroup::chain))

    return assets
        .map { asset -> AssetWithOffChainBalance(asset, asset.totalWithOffChain(offChainBalancesByFullAssetId)) }
        .groupBy { chainsById.getValue(it.asset.token.configuration.chainId) }
        .mapValues { (_, assets) ->
            assets.sortedWith(
                compareByDescending<AssetWithOffChainBalance> { it.totalWithOffChain.fiat }
                    .thenByDescending { it.totalWithOffChain.amount }
                    .thenByDescending { it.asset.token.configuration.isUtilityAsset } // utility assets first
                    .thenBy { it.asset.token.configuration.symbol }
            )
        }.mapKeys { (chain, assets) ->
            AssetGroup(
                chain = chain,
                groupBalanceFiat = assets.sumByBigDecimal { it.totalWithOffChain.fiat },
                zeroBalance = assets.any { it.totalWithOffChain.amount > BigDecimal.ZERO }
            )
        }.toSortedMap(assetGroupComparator)
}

private fun Asset.totalWithOffChain(offChainSource: Map<FullChainAssetId, BigInteger>): AssetWithOffChainBalance.TotalBalance {
    val onChainTotal = total
    val offChainTotal = offChainSource[token.configuration.fullId]
        ?.let(token::amountFromPlanks)
        .orZero()

    val overallTotal = onChainTotal + offChainTotal
    val overallFiat = token.priceOf(overallTotal)

    return AssetWithOffChainBalance.TotalBalance(overallTotal, overallFiat)
}
