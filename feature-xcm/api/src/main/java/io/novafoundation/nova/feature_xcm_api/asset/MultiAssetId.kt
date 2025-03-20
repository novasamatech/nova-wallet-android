package io.novafoundation.nova.feature_xcm_api.asset

import io.novafoundation.nova.common.data.network.runtime.binding.BalanceOf
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.VersionedToDynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

@JvmInline
value class MultiAssetId(val multiLocation: RelativeMultiLocation) : VersionedToDynamicScaleInstance {

    override fun toString(): String {
        return multiLocation.toString()
    }

    override fun toEncodableInstance(xcmVersion: XcmVersion): Any? {
        // V4 removed variants of MultiAssetId, leaving only flattened value of Concrete
        return if (xcmVersion >= XcmVersion.V4) {
            multiLocation.toEncodableInstance(xcmVersion)
        } else {
            DictEnum.Entry(
                name = "Concrete",
                value = multiLocation.toEncodableInstance(xcmVersion)
            )
        }
    }
}

fun MultiAssetId.withAmount(amount: BalanceOf): MultiAsset {
    return MultiAsset.from(multiLocation, amount)
}
