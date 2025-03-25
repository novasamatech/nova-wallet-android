package io.novafoundation.nova.feature_xcm_api.chain

import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.asLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class XcmChain(
    val parachainId: BigInteger?,
    val chain: Chain
)

fun XcmChain.absoluteLocation(): AbsoluteMultiLocation {
    val junctions = listOfNotNull(parachainId?.let(MultiLocation.Junction::ParachainId))
    return junctions.asLocation()
}
