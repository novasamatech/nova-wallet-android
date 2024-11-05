package io.novafoundation.nova.feature_assets.domain.assets.models

import io.novafoundation.nova.common.utils.MultiMapList
import io.novafoundation.nova.feature_assets.domain.common.AssetWithNetwork
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.domain.common.NetworkAssetGroup
import io.novafoundation.nova.feature_assets.domain.common.TokenAssetGroup

sealed interface AssetsByViewModeResult {

    class ByNetworks(val assets: MultiMapList<NetworkAssetGroup, AssetWithOffChainBalance>) : AssetsByViewModeResult

    class ByTokens(val tokens: MultiMapList<TokenAssetGroup, AssetWithNetwork>) : AssetsByViewModeResult
}

fun AssetsByViewModeResult.groupList(): List<Any> {
    return when (this) {
        is AssetsByViewModeResult.ByNetworks -> assets.keys.toList()
        is AssetsByViewModeResult.ByTokens -> tokens.keys.toList()
    }
}

fun MultiMapList<NetworkAssetGroup, AssetWithOffChainBalance>.byNetworks(): AssetsByViewModeResult {
    return AssetsByViewModeResult.ByNetworks(this)
}

fun MultiMapList<TokenAssetGroup, AssetWithNetwork>.byTokens(): AssetsByViewModeResult {
    return AssetsByViewModeResult.ByTokens(this)
}
