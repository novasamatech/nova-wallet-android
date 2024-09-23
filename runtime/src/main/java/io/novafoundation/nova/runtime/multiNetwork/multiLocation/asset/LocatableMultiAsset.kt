package io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset

import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.v3.LocatableMultiAssetV3
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.v4.LocatableMultiAssetV4

interface LocatableMultiAsset {

    val location: MultiLocation

    val assetId: MultiAssetId
}

sealed class VersionedLocatableMultiAsset(multiAssetId: LocatableMultiAsset) : LocatableMultiAsset by multiAssetId {

    class V3(val value: LocatableMultiAssetV3) : VersionedLocatableMultiAsset(value)

    class V4(val value: LocatableMultiAssetV4) : VersionedLocatableMultiAsset(value)
}
