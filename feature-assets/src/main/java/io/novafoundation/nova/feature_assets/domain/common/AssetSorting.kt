package io.novafoundation.nova.feature_assets.domain.common

import io.novafoundation.nova.common.utils.sumByBigDecimal
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

class AssetGroup(
    val chain: Chain,
    val groupBalanceFiat: BigDecimal,
    val zeroBalance: Boolean
)

fun groupAndSortAssetsByNetwork(
    assets: List<Asset>,
    chainsById: Map<String, Chain>
): Map<AssetGroup, List<Asset>> {
    val assetGroupComparator = compareByDescending(AssetGroup::groupBalanceFiat)
        .thenByDescending { it.zeroBalance } // non-zero balances first
        .then(Chain.defaultComparatorFrom(AssetGroup::chain))

    return assets.groupBy { chainsById.getValue(it.token.configuration.chainId) }
        .mapValues { (_, assets) ->
            assets.sortedWith(
                compareByDescending<Asset> { it.token.priceOf(it.total) }
                    .thenByDescending { it.total }
                    .thenByDescending { it.token.configuration.isUtilityAsset } // utility assets first
                    .thenBy { it.token.configuration.symbol }
            )
        }.mapKeys { (chain, assets) ->
            AssetGroup(
                chain = chain,
                groupBalanceFiat = assets.sumByBigDecimal { it.token.priceOf(it.total) },
                zeroBalance = assets.any { it.total > BigDecimal.ZERO }
            )
        }.toSortedMap(assetGroupComparator)
}
