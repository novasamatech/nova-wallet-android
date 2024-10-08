package io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.v3

import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.MultiAssetId

sealed class MultiAssetIdV3 : MultiAssetId {

    class Concrete(override val multiLocation: MultiLocation) : MultiAssetIdV3()
}
