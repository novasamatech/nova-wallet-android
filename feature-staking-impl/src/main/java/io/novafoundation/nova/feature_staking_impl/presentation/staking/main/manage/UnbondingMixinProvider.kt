package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.manage

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.mainStakingValidationFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ManageStakeMixinFactory(
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val stakeActionsValidations: Map<ManageStakeAction, StakeActionsValidationSystem>,
    private val router: StakingRouter,
) {

    fun create(
        errorDisplayer: (Throwable) -> Unit,
        stashState: StakingState.Stash,
        coroutineScope: CoroutineScope
    ): ManageStakeMixin.Presentation = ManageStakeProvider(
        validationExecutor = validationExecutor,
        resourceManager = resourceManager,
        router = router,
        stakeActionsValidations = stakeActionsValidations,
        errorDisplayer = errorDisplayer,
        stakingState = stashState,
        coroutineScope = coroutineScope
    )
}

private class ManageStakeProvider(
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val stakeActionsValidations: Map<ManageStakeAction, StakeActionsValidationSystem>,
    // From Parent Component
    private val errorDisplayer: (Throwable) -> Unit,
    private val stakingState: StakingState.Stash,
    coroutineScope: CoroutineScope,
) : ManageStakeMixin.Presentation,
    CoroutineScope by coroutineScope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(coroutineScope) {

    private val forbiddenStakeActions = determineForbiddenActions(stakingState)

    override val allowedStakeActions = ManageStakeAction.values().toSet() - forbiddenStakeActions

    override fun manageActionChosen(action: ManageStakeAction) {
        if (action in forbiddenStakeActions) return

        val validationSystem = stakeActionsValidations[action]

        if (validationSystem != null) {
            val payload = StakeActionsValidationPayload(stakingState)

            launch {
                validationExecutor.requireValid(
                    validationSystem = validationSystem,
                    payload = payload,
                    errorDisplayer = errorDisplayer,
                    validationFailureTransformerDefault = { mainStakingValidationFailure(it, resourceManager) },
                ) {
                    navigateToAction(action)
                }
            }
        } else {
            navigateToAction(action)
        }
    }

    private fun navigateToAction(action: ManageStakeAction) {
        when (action) {
            ManageStakeAction.PAYOUTS -> router.openPayouts()
            ManageStakeAction.BOND_MORE -> router.openBondMore()
            ManageStakeAction.UNBOND -> router.openSelectUnbond()
            ManageStakeAction.CONTROLLER -> router.openControllerAccount()
            ManageStakeAction.VALIDATORS -> router.openCurrentValidators()
            ManageStakeAction.REWARD_DESTINATION -> router.openChangeRewardDestination()
        }
    }

    private fun determineForbiddenActions(stashState: StakingState.Stash): Set<ManageStakeAction> = when(stashState) {
        is StakingState.Stash.Nominator -> emptySet()
        is StakingState.Stash.None -> setOf(ManageStakeAction.PAYOUTS)
        is StakingState.Stash.Validator -> setOf(ManageStakeAction.VALIDATORS)
    }
}
