package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.flowAction

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import kotlinx.coroutines.CoroutineScope

interface ReviewValidatorsFlowAction {

    suspend fun execute(coroutineScope: CoroutineScope, stakingOption: StakingOption)
}
