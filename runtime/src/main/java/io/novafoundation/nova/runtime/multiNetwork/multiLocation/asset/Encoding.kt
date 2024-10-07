package io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset

import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.v3.bindLocatableMultiAssetV3
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.asset.v4.bindLocatableMultiAssetV4

fun bindVersionedLocatableMultiAsset(decoded: Any?): VersionedLocatableMultiAsset {
    val asDictEnum = decoded.castToDictEnum()

    return when (asDictEnum.name) {
        "V3" -> VersionedLocatableMultiAsset.V3(bindLocatableMultiAssetV3(asDictEnum.value))
        "V4" -> VersionedLocatableMultiAsset.V4(bindLocatableMultiAssetV4(asDictEnum.value))
        else -> error("Unsupported LocatableMultiAsset version: ${asDictEnum.name}")
    }
}
