package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.parachain.turing

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.prepended
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.data.parachainStaking.turing.repository.TuringAutomationTasksRepository
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary.ParachainStakingUnbondPreliminaryValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.ManageStakeAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.parachain.ParachainStakeActionsComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

private const val YIELD_BOOST_ACTION = "YIELD_BOOST"

class TuringStakeActionsComponentFactory(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val resourceManager: ResourceManager,
    private val router: ParachainStakingRouter,
    private val validationExecutor: ValidationExecutor,
    private val unbondPreliminaryValidationSystem: ParachainStakingUnbondPreliminaryValidationSystem,
    private val turingAutomationTasksRepository: TuringAutomationTasksRepository,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): StakeActionsComponent = TuringStakeActionsComponent(
        delegatorStateUseCase = delegatorStateUseCase,
        resourceManager = resourceManager,
        stakingOption = stakingOption,
        hostContext = hostContext,
        router = router,
        validationExecutor = validationExecutor,
        unbondValidationSystem = unbondPreliminaryValidationSystem,
        turingAutomationTasksRepository = turingAutomationTasksRepository
    )
}

private class TuringStakeActionsComponent(
    delegatorStateUseCase: DelegatorStateUseCase,
    private val resourceManager: ResourceManager,
    private val router: ParachainStakingRouter,
    stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
    unbondValidationSystem: ParachainStakingUnbondPreliminaryValidationSystem,
    validationExecutor: ValidationExecutor,
    private val turingAutomationTasksRepository: TuringAutomationTasksRepository,
) : ParachainStakeActionsComponent(
    delegatorStateUseCase = delegatorStateUseCase,
    resourceManager = resourceManager,
    router = router,
    stakingOption = stakingOption,
    hostContext = hostContext,
    unbondValidationSystem = unbondValidationSystem,
    validationExecutor = validationExecutor
) {

    override fun stateFor(delegatorState: DelegatorState.Delegator): Flow<StakeActionsState> {
        return flow {
            val parachainStakingActions = availableParachainStakingActionsFor(delegatorState)
            emit(StakeActionsState((parachainStakingActions)))

            val allActionsState = turingAutomationTasksRepository.automationTasksFlow(delegatorState.chain.id, delegatorState.accountId).map { tasks ->
                val yieldBoostActive = tasks.isNotEmpty()
                val yieldBoostAction = ManageStakeAction.yieldBoost(yieldBoostActive)
                val allActions = parachainStakingActions.prepended(yieldBoostAction)

                StakeActionsState(allActions)
            }

            emitAll(allActionsState)
        }
    }

    override fun onAction(action: StakeActionsAction) {
        if (action is StakeActionsAction.ActionClicked && action.action.id == YIELD_BOOST_ACTION) {
            goToYieldBoost()
        } else {
            super.onAction(action)
        }
    }

    private fun goToYieldBoost() {
        router.openSetupYieldBoost()
    }

    private fun ManageStakeAction.Companion.yieldBoost(yieldBoostActive: Boolean): ManageStakeAction {
        return ManageStakeAction(
            id = YIELD_BOOST_ACTION,
            label = resourceManager.getString(R.string.staking_turing_yield_boost),
            iconRes = R.drawable.ic_chevron_up_circle_outline,
            badge = run {
                val resId = if (yieldBoostActive) R.string.common_on else R.string.common_off

                resourceManager.getString(resId)
            }
        )
    }
}
