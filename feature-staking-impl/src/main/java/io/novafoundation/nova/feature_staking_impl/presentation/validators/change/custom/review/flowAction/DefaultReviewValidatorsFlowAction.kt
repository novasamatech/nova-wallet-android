package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review

import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.flowAction.ReviewValidatorsFlowAction
import kotlinx.coroutines.CoroutineScope

class DefaultReviewValidatorsFlowAction(
    private val stakingRouter: StakingRouter
) : ReviewValidatorsFlowAction {

    override suspend fun execute(coroutineScope: CoroutineScope) {
        stakingRouter.openConfirmStaking()
    }
}
