package io.novafoundation.nova.feature_xcm_api.versions

interface VersionedToDynamicScaleInstance {

    fun toEncodableInstance(xcmVersion: XcmVersion): Any?
}
