package io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.v3

import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.LocatableMultiAsset

class LocatableMultiAssetV3(
    override val location: MultiLocation,
    override val assetId: MultiAssetIdV3
): LocatableMultiAsset
