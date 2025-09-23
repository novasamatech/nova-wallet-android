package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve

import io.novafoundation.nova.feature_xcm_api.chain.XcmChain
import io.novafoundation.nova.feature_xcm_api.chain.isRelay
import io.novafoundation.nova.feature_xcm_api.chain.isSystemChain
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation

sealed interface XcmTransferType {

    companion object {

        fun determineTransferType(
            usesTeleports: Boolean,
            originChain: XcmChain,
            destinationChain: XcmChain,
            reserve: TokenReserve
        ): XcmTransferType {
            val assetAbsoluteLocation = reserve.tokenLocation

            return when {
                usesTeleports -> Teleport(assetAbsoluteLocation)
                originChain.chain.id == reserve.reserveChainLocation.chainId -> Reserve.Origin(assetAbsoluteLocation)
                destinationChain.chain.id == reserve.reserveChainLocation.chainId -> Reserve.Destination(assetAbsoluteLocation)
                else -> Reserve.Remote(assetAbsoluteLocation, reserve.reserveChainLocation)
            }
        }

        fun isSystemTeleport(originXcmChain: XcmChain, destinationXcmChain: XcmChain): Boolean {
            val systemToRelay = originXcmChain.isSystemChain() && destinationXcmChain.isRelay()
            val relayToSystem = originXcmChain.isRelay() && destinationXcmChain.isSystemChain()
            val systemToSystem = originXcmChain.isSystemChain() && destinationXcmChain.isSystemChain()

            return systemToRelay || relayToSystem || systemToSystem
        }
    }

    val assetAbsoluteLocation: AbsoluteMultiLocation

    data class Teleport(override val assetAbsoluteLocation: AbsoluteMultiLocation) : XcmTransferType

    sealed interface Reserve : XcmTransferType {

        data class Origin(override val assetAbsoluteLocation: AbsoluteMultiLocation) : Reserve

        data class Destination(override val assetAbsoluteLocation: AbsoluteMultiLocation) : Reserve

        data class Remote(
            override val assetAbsoluteLocation: AbsoluteMultiLocation,
            val remoteReserveLocation: ChainLocation
        ) : Reserve
    }
}

fun XcmTransferType.remoteReserveLocation(): ChainLocation? {
    return (this as? XcmTransferType.Reserve.Remote)?.remoteReserveLocation
}

fun XcmTransferType.isRemoteReserve(): Boolean {
    return this is XcmTransferType.Reserve.Remote
}
