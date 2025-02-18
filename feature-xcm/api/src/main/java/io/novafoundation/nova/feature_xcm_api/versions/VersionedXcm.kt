package io.novafoundation.nova.feature_xcm_api.versions

import io.novafoundation.nova.common.data.network.runtime.binding.castToDictEnum
import io.novafoundation.nova.common.utils.scale.DynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novasama.substrate_sdk_android.runtime.definitions.types.composite.DictEnum

class VersionedXcm<T>(
    val xcm: T,
    val version: XcmVersion
)

@JvmName("toEncodableInstanceVersioned")
fun VersionedXcm<out VersionedToDynamicScaleInstance>.toEncodableInstance(): DictEnum.Entry<*> {
    return DictEnum.Entry(
        name = version.enumerationKey(),
        value = xcm.toEncodableInstance(version)
    )
}

fun VersionedXcm<out DynamicScaleInstance>.toEncodableInstance(): DictEnum.Entry<*> {
    return DictEnum.Entry(
        name = version.enumerationKey(),
        value = xcm.toEncodableInstance()
    )
}

fun <T> bindVersionedXcm(instance: Any?, inner: (Any?, xcmVersion: XcmVersion) -> T): VersionedXcm<T> {
    val versionEnum = instance.castToDictEnum()
    val xcmVersion = XcmVersion.fromEnumerationKey(versionEnum.name)

    return VersionedXcm(inner(versionEnum.value, xcmVersion), xcmVersion)
}

fun <T> T.versionedXcm(xcmVersion: XcmVersion): VersionedXcm<T> {
    return VersionedXcm(this, xcmVersion)
}

typealias VersionedXcmLocation = VersionedXcm<RelativeMultiLocation>
