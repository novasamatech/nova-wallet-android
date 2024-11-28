package io.novafoundation.nova.runtime.multiNetwork.multiLocation

enum class XcmVersion {
    V0, V1, V2, V3, V4;

    companion object {

        val DEFAULT = V2
    }
}


fun XcmVersion?.orDefault(): XcmVersion {
    return this ?: XcmVersion.DEFAULT
}
