package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review

import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter

interface ReviewValidatorsRouter {

    fun openNextScreen()
}

class SetupStakingReviewValidatorsRouter(
    private val stakingRouter: StakingRouter
) : ReviewValidatorsRouter {

    override fun openNextScreen() {
        stakingRouter.finishSetupValidatorsFlow()
    }
}

class ChangeStakingReviewValidatorsRouter(
    private val stakingRouter: StakingRouter
) : ReviewValidatorsRouter {

    override fun openNextScreen() {
        stakingRouter.openConfirmStaking()
    }
}
