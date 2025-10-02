package io.novafoundation.nova.runtime.network.updaters.multiChain

import io.novafoundation.nova.common.data.holders.ChainIdHolder
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain

class DelegateToTimelineChainIdHolder(
    private val sharedState: AnySelectedAssetOptionSharedState
) : ChainIdHolder {

    override suspend fun chainId(): String {
        return sharedState.chain().timelineChainIdOrSelf()
    }
}
