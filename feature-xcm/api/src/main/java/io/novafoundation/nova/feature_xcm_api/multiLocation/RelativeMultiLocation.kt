package io.novafoundation.nova.feature_xcm_api.multiLocation

import io.novafoundation.nova.feature_xcm_api.versions.VersionedToDynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion

class RelativeMultiLocation(
    val parents: Int,
    interior: Interior
) : MultiLocation(interior), VersionedToDynamicScaleInstance {

    override fun toEncodableInstance(xcmVersion: XcmVersion): Any {
        return toEncodableInstanceExt(xcmVersion)
    }
}

fun RelativeMultiLocation.isHere(): Boolean {
    return parents == 0 && interior.isHere()
}
