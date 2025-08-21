package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic

import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfigurationBase
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.TokenReserve
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.XcmTransferReserve
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.remoteReserveLocation
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
    val features: DynamicCrossChainTransferFeatures,
) : CrossChainTransferConfigurationBase {

    val assetAbsoluteLocation = reserve.tokenLocation

    val originChainLocation = originChain.chainLocation()
    val destinationChainLocation = destinationChain.chainLocation()
    val assetLocationOnOrigin = assetAbsoluteLocation.fromPointOfViewOf(originChain.absoluteLocation())

    val transferType = transferType()

    override val originChainId: ChainId = originChain.chain.id
    override val destinationChainId: ChainId = destinationChain.chain.id
    override val remoteReserveChainId: ChainId? = transferType.remoteReserveLocation()?.chainId

    private fun transferType(): XcmTransferReserve {
        return when {
            shouldUseTeleport() -> XcmTransferReserve.Teleport
            originChain.chain.id == reserve.reserveChainLocation.chainId -> XcmTransferReserve.Reserve.Origin
            destinationChain.chain.id == reserve.reserveChainLocation.chainId -> XcmTransferReserve.Reserve.Destination
            else -> XcmTransferReserve.Reserve.Remote(reserve.reserveChainLocation)
        }
    }
}

private fun DynamicCrossChainTransferConfiguration.shouldUseTeleport(): Boolean {
    val systemToRelay = originChain.isSystemChain() && destinationChain.isRelay()
    val relayToSystem = originChain.isRelay() && destinationChain.isSystemChain()
    val systemToSystem = originChain.isSystemChain() && destinationChain.isSystemChain()

    // We keep our hard-coded logic of determining teleports for system chains just in case
    // Script that detects `features.usesTeleport` breaks for some reason -
    // we will still be able to serve system chain transfers
    return systemToRelay || relayToSystem || systemToSystem || features.usesTeleport
}

fun DynamicCrossChainTransferConfiguration.destinationChainLocationOnOrigin(): RelativeMultiLocation {
    return destinationChainLocation.location.fromPointOfViewOf(originChainLocation.location)
}
