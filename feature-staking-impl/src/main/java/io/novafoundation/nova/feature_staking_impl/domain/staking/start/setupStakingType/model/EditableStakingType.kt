package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetails
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class EditableStakingType(
    val isAvailable: Boolean,
    val stakingTypeDetails: StakingTypeDetails,
    val stakingType: Chain.Asset.StakingType
)
