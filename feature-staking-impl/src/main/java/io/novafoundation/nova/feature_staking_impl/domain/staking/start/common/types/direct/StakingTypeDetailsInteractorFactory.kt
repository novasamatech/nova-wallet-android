package io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.direct

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.types.StakingTypeDetailsInteractor
import kotlinx.coroutines.CoroutineScope

interface StakingTypeDetailsInteractorFactory {

    suspend fun create(stakingOption: StakingOption, coroutineScope: CoroutineScope): StakingTypeDetailsInteractor
}
