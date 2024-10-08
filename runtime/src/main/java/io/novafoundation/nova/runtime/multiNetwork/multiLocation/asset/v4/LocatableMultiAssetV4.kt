package io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.v4

import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.LocatableMultiAsset

class LocatableMultiAssetV4(
    override val location: MultiLocation,
    override val assetId: MultiAssetIdV4
) : LocatableMultiAsset
