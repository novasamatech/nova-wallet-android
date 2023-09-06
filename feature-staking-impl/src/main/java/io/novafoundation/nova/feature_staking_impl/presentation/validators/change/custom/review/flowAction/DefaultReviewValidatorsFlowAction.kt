package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.flowAction

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import kotlinx.coroutines.CoroutineScope

class DefaultReviewValidatorsFlowAction(
    private val stakingRouter: StakingRouter
) : ReviewValidatorsFlowAction {

    override suspend fun execute(coroutineScope: CoroutineScope, stakingOption: StakingOption) {
        stakingRouter.openConfirmStaking()
    }
}
