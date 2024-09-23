package io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.v4

import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.bindMultiLocation

fun bindMultiAssetIdV4(decoded: Any?): MultiAssetIdV4 {
    return MultiAssetIdV4(bindMultiLocation(decoded))
}

fun bindLocatableMultiAssetV4(decoded: Any?): LocatableMultiAssetV4 {
    val asStruct = decoded.castToStruct()

    return LocatableMultiAssetV4(
        location = bindMultiLocation(asStruct["location"]),
        assetId = bindMultiAssetIdV4(asStruct["assetId"])
    )
}
