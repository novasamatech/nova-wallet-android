package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.omnipool.model

import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

typealias RemoteAndLocalId = Pair<HydraDxAssetId, FullChainAssetId>
typealias RemoteIdAndLocalAsset = Pair<HydraDxAssetId, Chain.Asset>
typealias RemoteAndLocalIdOptional = Pair<HydraDxAssetId, FullChainAssetId?>

@Suppress("UNCHECKED_CAST")
fun RemoteAndLocalIdOptional.flatten(): RemoteAndLocalId? {
    return second?.let { this as RemoteAndLocalId }
}

val RemoteAndLocalId.remoteId
    get() = first

val RemoteAndLocalId.localId
    get() = second
