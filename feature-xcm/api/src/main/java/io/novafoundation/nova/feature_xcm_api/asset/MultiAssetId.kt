package io.novafoundation.nova.feature_xcm_api.asset

import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.VersionedToDynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

@JvmInline
value class MultiAssetId(val multiLocation: RelativeMultiLocation) : VersionedToDynamicScaleInstance {

    override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
        return if (xcmVersion >= XcmVersion.V3) {
            multiLocation.toEncodableInstance(xcmVersion)
        } else {
            DictEnum.Entry(
                name = "Concrete",
                value = multiLocation.toEncodableInstance(xcmVersion)
            )
        }
    }
}
