package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve

import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class TokenReserve(
    val reserveChainLocation: ChainLocation,
    val tokenLocation: AbsoluteMultiLocation
)

fun TokenReserve.isRemote(origin: ChainId, destination: ChainId): Boolean {
    return origin != reserveChainLocation.chainId && destination != reserveChainLocation.chainId
}
