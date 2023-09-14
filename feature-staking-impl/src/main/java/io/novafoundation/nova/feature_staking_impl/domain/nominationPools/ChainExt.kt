package io.novafoundation.nova.feature_staking_impl.domain.nominationPools

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

fun Chain.Asset.findStakingTypeBackingNominationPools(): Chain.Asset.StakingType {
    return staking.first { it != Chain.Asset.StakingType.NOMINATION_POOLS }
}
