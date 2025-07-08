package io.novafoundation.nova.feature_xcm_api.builder

import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.versions.XcmVersion

interface XcmContext {

    val xcmVersion: XcmVersion

    val currentLocation: ChainLocation
}

fun XcmContext.localViewOf(location: AbsoluteMultiLocation): RelativeMultiLocation {
    return location.fromPointOfViewOf(currentLocation.location)
}

context(XcmContext)
fun AbsoluteMultiLocation.relativeToLocal(): RelativeMultiLocation {
    return localViewOf(this)
}
