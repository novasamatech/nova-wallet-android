package io.novafoundation.nova.feature_assets.domain.assets.models

import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup

sealed interface AssetFlowSearchResult {

    class ByNetworks(val assets: Map<NetworkAssetGroup, List<AssetWithOffChainBalance>>) : AssetFlowSearchResult

    class ByTokens(val tokens: List<TokenAssetGroup>) : AssetFlowSearchResult
}

fun AssetFlowSearchResult.toList(): List<Any> {
    return when (this) {
        is AssetFlowSearchResult.ByNetworks -> assets.keys.toList()
        is AssetFlowSearchResult.ByTokens -> tokens
    }
}
