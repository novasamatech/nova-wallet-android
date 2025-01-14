package io.novafoundation.nova.feature_xcm_api.asset

import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.feature_xcm_api.multiLocation.bindMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion
import io.novafoundation.nova.feature_xcm_api.versions.bindVersionedXcm

fun bindVersionedLocatableMultiAsset(decoded: Any?): VersionedXcm<LocatableMultiAsset> {
    return bindVersionedXcm(decoded, ::bindLocatableMultiAsset)
}

fun bindLocatableMultiAsset(decoded: Any?, xcmVersion: XcmVersion): LocatableMultiAsset {
    val asStruct = decoded.castToStruct()

    return LocatableMultiAsset(
        location = bindMultiLocation(asStruct["location"]),
        assetId = bindMultiAssetId(asStruct["assetId"], xcmVersion)
    )
}

fun bindMultiAssetId(decoded: Any?, xcmVersion: XcmVersion): MultiAssetId {
    val locationInstance = if (xcmVersion >= XcmVersion.V3) {
        decoded
    } else {
        extractConcreteLocation(decoded)
    }

    return MultiAssetId(bindMultiLocation(locationInstance))
}

private fun extractConcreteLocation(decoded: Any?): Any? {
    val variant = decoded.castToDictEnum()
    require(variant.name == "Concrete") {
        "Asset ids besides Concrete are not supported"
    }

    return variant.value
}
