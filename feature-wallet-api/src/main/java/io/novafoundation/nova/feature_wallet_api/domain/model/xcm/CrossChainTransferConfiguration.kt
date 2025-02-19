package io.novafoundation.nova.feature_wallet_api.domain.model.xcm

import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.dynamic.DynamicCrossChainTransferConfiguration
import io.novafoundation.nova.feature_wallet_api.domain.model.xcm.legacy.LegacyCrossChainTransferConfiguration
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

    val originChainId: ChainId

    val destinationChainId: ChainId

    val remoteReserveChainId: ChainId?
}
