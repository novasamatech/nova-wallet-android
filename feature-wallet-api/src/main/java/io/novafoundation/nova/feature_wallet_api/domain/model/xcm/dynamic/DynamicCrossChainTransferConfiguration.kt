package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic

import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfigurationBase
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserve
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.XcmTransferReserve
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.isRemote
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.chain.XcmChain
import io.novafoundation.nova.feature_xcm_api.chain.absoluteLocation
import io.novafoundation.nova.feature_xcm_api.chain.chainLocation
import io.novafoundation.nova.feature_xcm_api.chain.isRelay
import io.novafoundation.nova.feature_xcm_api.chain.isSystemChain
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class DynamicCrossChainTransferConfiguration(
    val originChain: XcmChain,
    val destinationChain: XcmChain,
    val originChainAsset: Chain.Asset,
    val reserve: TokenReserve,
    val hasDeliveryFee: Boolean,
    val supportsXcmExecute: Boolean
) : CrossChainTransferConfigurationBase {

    val assetAbsoluteLocation = reserve.location

    val originChainLocation = originChain.chainLocation()
    val destinationChainLocation = destinationChain.chainLocation()
    val assetLocationOnOrigin = reserve.location.fromPointOfViewOf(originChain.absoluteLocation())

    val remoteReserveChainLocation = if (reserve.isRemote(originChain.chain.id, destinationChain.chain.id)) {
        ChainLocation(reserve.chainId, reserve.location)
    } else {
        null
    }

    override val originChainId: ChainId = originChain.chain.id
    override val destinationChainId: ChainId = destinationChain.chain.id
    override val remoteReserveChainId: ChainId? = remoteReserveChainLocation?.chainId
}

fun DynamicCrossChainTransferConfiguration.transferType(): XcmTransferReserve {
    return when {
        shouldUseTeleport() -> XcmTransferReserve.TELEPORT
        originChainId == reserve.chainId -> XcmTransferReserve.ORIGIN_RESERVE
        destinationChainId == reserve.chainId -> XcmTransferReserve.DESTINATION_RESERVE
        else -> XcmTransferReserve.REMOTE_RESERVE
    }
}

private fun DynamicCrossChainTransferConfiguration.shouldUseTeleport(): Boolean {
    val systemToRelay =  originChain.isSystemChain() && destinationChain.isRelay()
    val relayToSystem = originChain.isRelay() && destinationChain.isSystemChain()

    return systemToRelay || relayToSystem
}

fun DynamicCrossChainTransferConfiguration.destinationChainLocationOnOrigin(): RelativeMultiLocation {
    return destinationChainLocation.location.fromPointOfViewOf(originChainLocation.location)
}
