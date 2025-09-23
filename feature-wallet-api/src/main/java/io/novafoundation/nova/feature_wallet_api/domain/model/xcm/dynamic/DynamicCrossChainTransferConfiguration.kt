package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic

import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfigurationBase
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.XcmTransferType
import io.novafoundation.nova.feature_xcm_api.chain.XcmChain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class DynamicCrossChainTransferConfiguration(
    override val originChain: XcmChain,
    override val destinationChain: XcmChain,
    override val transferType: XcmTransferType,
    override val originChainAsset: Chain.Asset,
    val features: DynamicCrossChainTransferFeatures,
) : CrossChainTransferConfigurationBase {

    override fun debugExtraInfo(): String {
        return "features=${features}"
    }
}
