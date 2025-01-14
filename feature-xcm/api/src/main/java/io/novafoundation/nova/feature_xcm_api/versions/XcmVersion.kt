package io.novafoundation.nova.feature_xcm_api.versions

enum class XcmVersion(val version: Int) {
    V0(0), V1(1), V2(2), V3(3), V4(4);

    companion object {

        fun fromVersion(version: Int): XcmVersion {
            return values().find { it.version == version }
                ?: error("Unknown xcm version: $version")
        }

        val GLOBAL_DEFAULT = V2
    }
}

// Return xcm version from a enumeration key in form of "V{version}"
fun XcmVersion.Companion.fromEnumerationKey(enumerationKey: String): XcmVersion {
    val withoutPrefix = enumerationKey.removePrefix("V")
    val version = withoutPrefix.toInt()
    return fromVersion(version)
}


fun XcmVersion?.orDefault(): XcmVersion {
    return this ?: XcmVersion.GLOBAL_DEFAULT
}

fun XcmVersion.enumerationKey(): String {
    return "V${version}"
}
