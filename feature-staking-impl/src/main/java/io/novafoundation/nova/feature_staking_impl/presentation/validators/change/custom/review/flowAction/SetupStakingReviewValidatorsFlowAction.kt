package io.novafoundation.nova.feature_staking_impl.presentation.validators.change.custom.review.flowAction

import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupStakingType.SetupStakingTypeSelectionMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.SetupStakingSharedState
import io.novafoundation.nova.feature_staking_impl.presentation.validators.change.getSelectedValidators
import kotlinx.coroutines.CoroutineScope

class SetupStakingReviewValidatorsFlowAction(
    private val stakingRouter: StakingRouter,
    private val sharedStateSetup: SetupStakingSharedState,
    private val setupStakingTypeSelectionMixinFactory: SetupStakingTypeSelectionMixinFactory
) : ReviewValidatorsFlowAction {

    override suspend fun execute(coroutineScope: CoroutineScope, stakingOption: StakingOption) {
        val setupStakingTypeSelectionMixin = setupStakingTypeSelectionMixinFactory.create(coroutineScope)
        val selectedValidators = sharedStateSetup.getSelectedValidators()
        setupStakingTypeSelectionMixin.selectValidatorsAndApply(selectedValidators, stakingOption)

        stakingRouter.finishSetupValidatorsFlow()
    }
}
