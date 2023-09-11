package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupStakingType

import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.model.ValidatedStakingTypeDetails
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group

fun getEditableStakingTypeComparator(): Comparator<ValidatedStakingTypeDetails> {
    return compareBy {
        when (it.stakingTypeDetails.stakingType.group()) {
            StakingTypeGroup.NOMINATION_POOL -> 0
            StakingTypeGroup.RELAYCHAIN -> 1
            else -> 3
        }
    }
}
