package io.novafoundation.nova.feature_xcm_api.message

import io.novafoundation.nova.common.utils.scale.DynamicScaleInstance
import io.novafoundation.nova.feature_xcm_api.versions.VersionedXcm
import io.novafoundation.nova.feature_xcm_api.versions.bindVersionedXcm

typealias XcmMessageRaw = DynamicScaleInstance
typealias VersionedRawXcmMessage = VersionedXcm<XcmMessageRaw>

fun bindVersionedRawXcmMessage(decodedInstance: Any?) = bindVersionedXcm(decodedInstance) { inner, _ ->
    DynamicScaleInstance(inner)
}
