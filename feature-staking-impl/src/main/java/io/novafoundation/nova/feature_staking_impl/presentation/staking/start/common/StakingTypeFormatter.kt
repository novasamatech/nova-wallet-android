package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun ResourceManager.formatStakingTypeLabel(stakingType: Chain.Asset.StakingType): String {
    return when (stakingType.group()) {
        StakingTypeGroup.RELAYCHAIN -> getString(R.string.setup_staking_type_direct_staking)
        StakingTypeGroup.NOMINATION_POOL -> getString(R.string.setup_staking_type_pool_staking)
        StakingTypeGroup.UNSUPPORTED, StakingTypeGroup.PARACHAIN, StakingTypeGroup.MYTHOS -> error("Not yet available")
    }
}
