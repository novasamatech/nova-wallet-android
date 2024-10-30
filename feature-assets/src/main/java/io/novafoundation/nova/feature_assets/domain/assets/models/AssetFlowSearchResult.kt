package io.novafoundation.nova.feature_assets.domain.assets.models

import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup

sealed interface AssetFlowSearchResult {

    class ByNetworks(val assets: MultiMapList<NetworkAssetGroup, AssetWithOffChainBalance>) : AssetFlowSearchResult

    class ByTokens(val tokens: MultiMapList<TokenAssetGroup, AssetWithNetwork>) : AssetFlowSearchResult
}

fun AssetFlowSearchResult.groupList(): List<Any> {
    return when (this) {
        is AssetFlowSearchResult.ByNetworks -> assets.keys.toList()
        is AssetFlowSearchResult.ByTokens -> tokens.keys.toList()
    }
}
