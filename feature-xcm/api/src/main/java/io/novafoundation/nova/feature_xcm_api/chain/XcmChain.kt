package io.novafoundation.nova.feature_xcm_api.chain

import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.chainLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

class XcmChain(
    val parachainId: BigInteger?,
    val chain: Chain
)

fun XcmChain.absoluteLocation(): AbsoluteMultiLocation {
    return AbsoluteMultiLocation.chainLocation(parachainId)
}

fun XcmChain.isRelay(): Boolean {
    return parachainId == null
}

fun XcmChain.isSystemChain(): Boolean {
    return parachainId != null && parachainId.toInt() in 1000 until 2000
}
