package io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.v3

import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.bindMultiLocation

fun bindMultiAssetIdV3(decoded: Any?): MultiAssetIdV3 {
    val variant = decoded.castToDictEnum()

    return when(variant.name) {
        "Concrete" -> MultiAssetIdV3.Concrete(bindMultiLocation(variant.value))
        else -> error("Asset ids besides Concrete are not supported")
    }
}

fun bindLocatableMultiAssetV3(decoded: Any?): LocatableMultiAssetV3 {
    val asStruct = decoded.castToStruct()

    return LocatableMultiAssetV3(
        location = bindMultiLocation(asStruct["location"]),
        assetId = bindMultiAssetIdV3(asStruct["asset_id"])
    )
}
