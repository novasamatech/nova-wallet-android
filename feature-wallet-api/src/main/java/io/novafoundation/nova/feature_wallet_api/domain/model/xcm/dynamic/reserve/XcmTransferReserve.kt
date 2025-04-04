package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve

import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation

sealed interface XcmTransferReserve {
    object Teleport : XcmTransferReserve

    sealed interface Reserve : XcmTransferReserve {
        object Origin : Reserve

        object Destination : Reserve

        class Remote(val remoteReserveLocation: ChainLocation) : Reserve
    }
}


fun XcmTransferReserve.remoteReserveLocation(): ChainLocation? {
    return (this as? XcmTransferReserve.Reserve.Remote)?.remoteReserveLocation
}
