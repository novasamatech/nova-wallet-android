package io.novafoundation.nova.feature_wallet_impl.data.network.crosschain.dynamic

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class DynamicXcmFeeMetadata(
    val xcmFeesSourceByChain: Map<ChainId, XcmFeesSource>
)
