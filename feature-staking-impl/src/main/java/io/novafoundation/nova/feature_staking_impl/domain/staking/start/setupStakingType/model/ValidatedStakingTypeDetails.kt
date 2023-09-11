package io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetails

class ValidatedStakingTypeDetails(
    val isAvailable: Boolean,
    val stakingTypeDetails: StakingTypeDetails
)
