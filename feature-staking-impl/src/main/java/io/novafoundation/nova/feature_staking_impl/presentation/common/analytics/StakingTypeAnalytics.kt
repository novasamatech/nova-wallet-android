package io.novafoundation.nova.feature_staking_impl.presentation.common.analytics

import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

const val ANALYTICS_STAKING_TYPE_DIRECT = "direct"
const val ANALYTICS_STAKING_TYPE_POOL = "pool"
const val ANALYTICS_STAKING_TYPE_MYTHOS = "mythos"
const val ANALYTICS_STAKING_TYPE_UNSUPPORTED = "unsupported"

fun Chain.Asset.StakingType.toAnalyticsStakingType(): String {
    return when (group()) {
        StakingTypeGroup.RELAYCHAIN, StakingTypeGroup.PARACHAIN -> ANALYTICS_STAKING_TYPE_DIRECT
        StakingTypeGroup.NOMINATION_POOL -> ANALYTICS_STAKING_TYPE_POOL
        StakingTypeGroup.MYTHOS -> ANALYTICS_STAKING_TYPE_MYTHOS
        StakingTypeGroup.UNSUPPORTED -> ANALYTICS_STAKING_TYPE_UNSUPPORTED
    }
}
