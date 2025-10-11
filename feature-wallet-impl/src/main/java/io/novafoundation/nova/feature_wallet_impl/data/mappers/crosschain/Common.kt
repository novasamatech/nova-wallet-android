package io.novafoundation.nova.feature_wallet_impl.data.mappers.crosschain

import io.novafoundation.nova.common.utils.asGsonParsedNumber
import io.novafoundation.nova.feature_xcm_api.config.remote.JunctionsRemote
import io.novafoundation.nova.feature_xcm_api.config.remote.toInterior
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation

private const val PARENTS = "parents"

fun mapJunctionsRemoteToMultiLocation(
    junctionsRemote: JunctionsRemote
): RelativeMultiLocation {
    return if (PARENTS in junctionsRemote) {
        val parents = junctionsRemote.getValue(PARENTS).asGsonParsedNumber().toInt()
        val withoutParents = junctionsRemote - PARENTS

        RelativeMultiLocation(
            parents = parents,
            interior = withoutParents.toInterior()
        )
    } else {
        RelativeMultiLocation(
            parents = 0,
            interior = junctionsRemote.toInterior()
        )
    }
}
