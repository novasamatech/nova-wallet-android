package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve

import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class TokenReserve(
    val chainId: ChainId,
    val location: AbsoluteMultiLocation
)

fun TokenReserve.isRemote(origin: ChainId, destination: ChainId): Boolean {
    return origin != chainId && destination != chainId
}
