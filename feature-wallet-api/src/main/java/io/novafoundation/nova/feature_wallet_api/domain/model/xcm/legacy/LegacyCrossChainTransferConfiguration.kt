package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy

import io.novafoundation.nova.common.data.network.runtime.binding.Weight
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfigurationBase
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.XcmTransferType
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransfersConfiguration.XcmFee
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class LegacyCrossChainTransferConfiguration(
    override val originChain: Chain,
    override val destinationChain: Chain,
    override val originChainAsset: Chain.Asset,
    override val transferType: XcmTransferType,

    override val destinationChainLocation: ChainLocation,
    override val originChainLocation: ChainLocation,

    // We cannot use assetLocationOnOrigin() for legacy since legacy config supports some weird cases
    // like Acala seeing ACA as parents: 1, junctions: [ParachainId(Acala)]
    val assetLocation: RelativeMultiLocation,
    // Those two field is duplicated by CrossChainTransferConfigurationBase info
    // But we do not refactor it to avoid unnecessary scope exposure
    val reserveChainLocation: RelativeMultiLocation,

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
