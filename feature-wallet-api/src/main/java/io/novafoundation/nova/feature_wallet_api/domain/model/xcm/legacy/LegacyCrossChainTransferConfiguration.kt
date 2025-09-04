package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfigurationBase
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration.XcmFee
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class LegacyCrossChainTransferConfiguration(
    override val originChainId: ChainId,
    val assetLocation: RelativeMultiLocation,
    val reserveChainLocation: RelativeMultiLocation,
    val destinationChainLocation: RelativeMultiLocation,
    val destinationFee: CrossChainFeeConfiguration,
    val reserveFee: CrossChainFeeConfiguration?,
    val transferType: XcmTransferType
) : CrossChainTransferConfigurationBase {

    override val destinationChainId: ChainId
        get() = destinationFee.to.chainId

    override val remoteReserveChainId: ChainId?
        get() = reserveFee?.to?.chainId
}

class CrossChainFeeConfiguration(
    val from: From,
    val to: To
) {

    class From(val chainId: ChainId, val deliveryFeeConfiguration: DeliveryFeeConfiguration?)

    class To(
        val chainId: ChainId,
        val instructionWeight: Weight,
        val xcmFeeType: XcmFee<List<XCMInstructionType>>
    )
}
