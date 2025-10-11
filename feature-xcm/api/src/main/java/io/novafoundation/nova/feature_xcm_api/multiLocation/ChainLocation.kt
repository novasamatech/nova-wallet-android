package io.novafoundation.nova.feature_xcm_api.multiLocation

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

data class ChainLocation(
    val chainId: ChainId,
    val location: AbsoluteMultiLocation
)
