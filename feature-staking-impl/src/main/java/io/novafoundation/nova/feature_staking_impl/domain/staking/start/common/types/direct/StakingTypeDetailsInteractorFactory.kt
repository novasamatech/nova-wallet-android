package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct

import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsInteractor

interface StakingTypeDetailsInteractorFactory {

    suspend fun create(stakingOption: StakingOption, computationalScope: ComputationalScope): StakingTypeDetailsInteractor
}
