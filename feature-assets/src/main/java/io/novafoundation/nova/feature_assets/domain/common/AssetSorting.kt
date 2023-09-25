package io.novafoundation.nova.feature_assets.domain.common

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.sumByBigDecimal
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigDecimal

class AssetGroup(
    val chain: Chain,
    val groupTotalBalanceFiat: BigDecimal,
    val groupTransferableBalanceFiat: BigDecimal,
    val zeroBalance: Boolean
)

class AssetWithOffChainBalance(
    val asset: Asset,
    val balanceWithOffchain: Balance,
) {

    class Balance(
        val total: Amount,
        val transferable: Amount
    )
}

class Amount(
    val amount: BigDecimal,
    val fiat: BigDecimal
)

fun groupAndSortAssetsByNetwork(
    assets: List<Asset>,
    externalBalances: Map<FullChainAssetId, Balance>,
    chainsById: Map<String, Chain>
): Map<AssetGroup, List<AssetWithOffChainBalance>> {
    val assetGroupComparator = compareByDescending(AssetGroup::groupTotalBalanceFiat)
        .thenByDescending { it.zeroBalance } // non-zero balances first
        .then(Chain.defaultComparatorFrom(AssetGroup::chain))

    return assets
        .map { asset -> AssetWithOffChainBalance(asset, asset.totalWithOffChain(externalBalances)) }
        .groupBy { chainsById.getValue(it.asset.token.configuration.chainId) }
        .mapValues { (_, assets) ->
            assets.sortedWith(
                compareByDescending<AssetWithOffChainBalance> { it.balanceWithOffchain.total.fiat }
                    .thenByDescending { it.balanceWithOffchain.total.amount }
                    .thenByDescending { it.asset.token.configuration.isUtilityAsset } // utility assets first
                    .thenBy { it.asset.token.configuration.symbol }
            )
        }.mapKeys { (chain, assets) ->
            AssetGroup(
                chain = chain,
                groupTotalBalanceFiat = assets.sumByBigDecimal { it.balanceWithOffchain.total.fiat },
                groupTransferableBalanceFiat = assets.sumByBigDecimal { it.balanceWithOffchain.transferable.fiat },
                zeroBalance = assets.any { it.balanceWithOffchain.total.amount > BigDecimal.ZERO }
            )
        }.toSortedMap(assetGroupComparator)
}

private fun Asset.totalWithOffChain(externalBalances: Map<FullChainAssetId, Balance>): AssetWithOffChainBalance.Balance {
    val onChainTotal = total
    val offChainTotal = externalBalances[token.configuration.fullId]
        ?.let(token::amountFromPlanks)
        .orZero()

    val overallTotal = onChainTotal + offChainTotal
    val overallFiat = token.amountToFiat(overallTotal)

    return AssetWithOffChainBalance.Balance(
        Amount(overallTotal, overallFiat),
        Amount(transferable, token.amountToFiat(transferable))
    )
}
