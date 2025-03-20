package io.novafoundation.nova.feature_xcm_api.multiLocation

import io.novafoundation.nova.feature_xcm_api.asset.MultiAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class AssetLocation(
    val assetId: FullChainAssetId,
    val location: AbsoluteMultiLocation
)

fun AssetLocation.multiAssetIdOn(chainLocation: ChainLocation): MultiAssetId {
    val relativeMultiLocation = location.fromPointOfViewOf(chainLocation.location)
    return MultiAssetId(relativeMultiLocation)
}
