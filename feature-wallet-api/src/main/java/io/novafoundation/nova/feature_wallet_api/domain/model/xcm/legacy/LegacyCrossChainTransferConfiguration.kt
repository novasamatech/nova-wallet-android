package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfigurationBase
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.XcmTransferType
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration.XcmFee
import io.novafoundation.nova.feature_xcm_api.chain.XcmChain
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class LegacyCrossChainTransferConfiguration(
    override val originChain: XcmChain,
    override val destinationChain: XcmChain,
    override val originChainAsset: Chain.Asset,
    override val transferType: XcmTransferType,

    // Those 3 fields are duplicated by CrossChainTransferConfigurationBase extensions
    // But we do not refactor it to avoid unnecessary scope exposure
    val assetLocation: RelativeMultiLocation,
    val reserveChainLocation: RelativeMultiLocation,
    val destinationChainLocation: RelativeMultiLocation,

    val destinationFee: CrossChainFeeConfiguration,
    val reserveFee: CrossChainFeeConfiguration?,
    val transferMethod: LegacyXcmTransferMethod,
) : CrossChainTransferConfigurationBase {

    override fun debugExtraInfo(): String {
       return "transferMethod=$transferMethod"
    }
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
