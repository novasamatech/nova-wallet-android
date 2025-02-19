package io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic

import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.CrossChainTransferConfigurationBase
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class DynamicCrossChainTransferConfiguration(
    override val originChainId: ChainId,
    override val destinationChainId: ChainId,
    override val remoteReserveChainId: ChainId?,
    val assetLocation: RelativeMultiLocation,
    val destinationChainLocation: RelativeMultiLocation,
    val hasDeliveryFee: Boolean
): CrossChainTransferConfigurationBase
