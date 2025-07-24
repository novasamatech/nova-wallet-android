package io.novafoundation.nova.feature_staking_impl.data

import io.novafoundation.nova.common.data.holders.ChainIdHolder
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.state.chain
import javax.inject.Inject

@FeatureScope
class TimelineDelegatingChainIdHolder @Inject constructor(
    private val stakingSharedState: StakingSharedState
) : ChainIdHolder {

    override suspend fun chainId(): String {
        return stakingSharedState.chain().timelineChainIdOrSelf()
    }
}
