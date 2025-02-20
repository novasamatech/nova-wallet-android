package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic

import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfigurationBase
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class DynamicCrossChainTransferConfiguration(
    val assetLocationOnOrigin: RelativeMultiLocation,
    val originChainLocation: ChainLocation,
    val destinationChainLocation: ChainLocation,
    val remoteReserveChainLocation: ChainLocation?,
) : CrossChainTransferConfigurationBase {

    override val originChainId: ChainId = originChainLocation.chainId
    override val destinationChainId: ChainId = destinationChainLocation.chainId
    override val remoteReserveChainId: ChainId? = remoteReserveChainLocation?.chainId

    class ChainLocation(
        val chainId: ChainId,
        val location: AbsoluteMultiLocation
    )
}

fun DynamicCrossChainTransferConfiguration.destinationChainLocationOnOrigin(): RelativeMultiLocation {
    return destinationChainLocation.location.fromPointOfViewOf(originChainLocation.location)
}
