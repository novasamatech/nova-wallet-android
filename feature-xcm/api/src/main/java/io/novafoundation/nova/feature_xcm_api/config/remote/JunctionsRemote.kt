package io.novafoundation.nova.feature_xcm_api.config.remote

import io.novafoundation.nova.common.utils.asGsonParsedNumber
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation.Junction
import io.novafoundation.nova.feature_xcm_api.multiLocation.toInterior

typealias JunctionsRemote = Map<String, Any?>

fun JunctionsRemote.toAbsoluteLocation(): AbsoluteMultiLocation {
    return AbsoluteMultiLocation(toInterior())
}

fun JunctionsRemote.toInterior(): MultiLocation.Interior {
    return map { (type, value) -> mapJunctionFromRemote(type, value) }
        .toInterior()
}

private fun mapJunctionFromRemote(type: String, value: Any?): Junction {
    return when (type) {
        "parachainId" -> Junction.ParachainId(value.asGsonParsedNumber())
        "generalKey" -> Junction.GeneralKey(value as String)
        "palletInstance" -> Junction.PalletInstance(value.asGsonParsedNumber())
        "generalIndex" -> Junction.GeneralIndex(value.asGsonParsedNumber())
        else -> throw IllegalArgumentException("Unknown junction type: $type")
    }
}
