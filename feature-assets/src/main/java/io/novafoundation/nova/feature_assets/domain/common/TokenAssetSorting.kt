package io.novafoundation.nova.feature_assets.domain.common

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.ext.defaultComparatorFrom
import io.novafoundation.nova.runtime.ext.isUtilityAsset
import io.novafoundation.nova.runtime.ext.normalize
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigDecimal

class TokenAssetGroup(
    val tokenInfo: TokenInfo,
    val groupBalance: AssetBalance,
    val itemsCount: Int
) {

    val groupId: String = tokenInfo.symbol.value

    data class TokenInfo(
        val icon: String?,
        val token: Token
    ) {
        val symbol = token.configuration.symbol.normalize()
        val currency = token.currency
        val coinRate = token.coinRate
    }
}

class AssetWithNetwork(
    val chain: Chain,
    val asset: Asset,
    val balanceWithOffChain: AssetBalance,
)

fun groupAndSortAssetsByToken(
    assets: List<Asset>,
    externalBalances: Map<FullChainAssetId, Balance>,
    chainsById: Map<String, Chain>,
    assetGroupComparator: Comparator<TokenAssetGroup> = getTokenAssetGroupBaseComparator(),
    assetComparator: Comparator<AssetWithNetwork> = getTokenAssetBaseComparator()
): Map<TokenAssetGroup, List<AssetWithNetwork>> {
    return assets
        .filter { chainsById.containsKey(it.token.configuration.chainId) }
        .map { asset -> AssetWithNetwork(chainsById.getValue(asset.token.configuration.chainId), asset, asset.totalWithOffChain(externalBalances)) }
        .groupBy { mapToTokenGroup(it) }
        .mapValues { (_, assets) -> assets.sortedWith(assetComparator) }
        .mapKeys { (tokenWrapper, assets) ->
            TokenAssetGroup(
                tokenInfo = tokenWrapper.tokenInfo,
                groupBalance = assets.fold(AssetBalance.ZERO) { acc, element -> acc + element.balanceWithOffChain },
                itemsCount = assets.size
            )
        }.toSortedMap(assetGroupComparator)
}

fun getTokenAssetBaseComparator(
    comparing: (AssetWithNetwork) -> Comparable<*> = { it.balanceWithOffChain.total.amount }
): Comparator<AssetWithNetwork> {
    return compareByDescending(comparing)
        .thenByDescending { it.asset.token.configuration.isUtilityAsset } // utility assets first
        .thenBy { it.asset.token.configuration.symbol.value }
        .then(Chain.defaultComparatorFrom(AssetWithNetwork::chain))
}

fun getTokenAssetGroupBaseComparator(
    comparing: (TokenAssetGroup) -> Comparable<*> = { it.groupBalance.total.fiat }
): Comparator<TokenAssetGroup> {
    return compareByDescending(comparing)
        .thenByDescending { it.groupBalance.total.amount > BigDecimal.ZERO } // non-zero balances first
        .then(TokenSymbol.defaultComparatorFrom { it.tokenInfo.symbol })
}

private fun mapToTokenGroup(it: AssetWithNetwork) = TokenGroupWrapper(
    TokenAssetGroup.TokenInfo(
        it.asset.token.configuration.icon,
        it.asset.token
    )
)

// Helper class to group items by symbol only
private class TokenGroupWrapper(val tokenInfo: TokenAssetGroup.TokenInfo) {

    override fun equals(other: Any?): Boolean {
        return other is TokenGroupWrapper && tokenInfo.symbol == other.tokenInfo.symbol
    }

    override fun hashCode(): Int {
        return tokenInfo.symbol.hashCode()
    }
}
