package io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset

import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.v3.MultiAssetIdV3
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.v4.MultiAssetIdV4

interface MultiAssetId {

    val multiLocation: MultiLocation
}

sealed class VersionedMultiAssetId(multiAssetId: MultiAssetId) : MultiAssetId by multiAssetId {

    class V3(val value: MultiAssetIdV3) : VersionedMultiAssetId(value)

    class V4(val value: MultiAssetIdV4) : VersionedMultiAssetId(value)
}
