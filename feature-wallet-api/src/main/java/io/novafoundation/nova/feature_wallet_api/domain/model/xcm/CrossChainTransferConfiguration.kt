package io.novafoundation.nova.feature_wallet_api.domain.model.xcm

import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.reserve.XcmTransferType
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransferConfiguration
import io.novafoundation.nova.feature_xcm_api.chain.XcmChain
import io.novafoundation.nova.feature_xcm_api.chain.chainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.ChainLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

sealed interface CrossChainTransferConfiguration : CrossChainTransferConfigurationBase {

    class Legacy(val config: LegacyCrossChainTransferConfiguration) :
        CrossChainTransferConfiguration,
        CrossChainTransferConfigurationBase by config

    class Dynamic(val config: DynamicCrossChainTransferConfiguration) :
        CrossChainTransferConfiguration,
        CrossChainTransferConfigurationBase by config
}

interface CrossChainTransferConfigurationBase {

    val originChain: XcmChain

    val destinationChain: XcmChain

    val originChainAsset: Chain.Asset

    val transferType: XcmTransferType

    /**
     * Any info usefully for logging besides fields [CrossChainTransferConfigurationBase] already expose
     */
    fun debugExtraInfo(): String
}

val CrossChainTransferConfigurationBase.originChainLocation: ChainLocation
    get() = originChain.chainLocation()

val CrossChainTransferConfigurationBase.destinationChainLocation: ChainLocation
    get() = destinationChain.chainLocation()

val CrossChainTransferConfigurationBase.originChainId: ChainId
    get() = originChainLocation.chainId

val CrossChainTransferConfigurationBase.destinationChainId: ChainId
    get() = destinationChainLocation.chainId

fun CrossChainTransferConfigurationBase.assetLocationOnOrigin(): RelativeMultiLocation {
    return transferType.assetAbsoluteLocation.fromPointOfViewOf(originChainLocation.location)
}

fun CrossChainTransferConfigurationBase.destinationChainLocationOnOrigin(): RelativeMultiLocation {
    return destinationChainLocation.location.fromPointOfViewOf(originChainLocation.location)
}
