package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.relaychain

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_CONTROLLER
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_PAYOUTS
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_REWARD_DESTINATION
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_BOND_MORE
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_UNBOND
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_VALIDATORS
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.ManageStakeAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.bondMore
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.controller
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.payouts
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.rewardDestination
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.unbond
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.validators
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.mainStakingValidationFailure
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

class RelaychainStakeActionsComponentFactory(
    private val stakingInteractor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val stakeActionsValidations: Map<String, StakeActionsValidationSystem>,
    private val router: StakingRouter,
) {

    fun create(
        assetWithChain: SingleAssetSharedState.AssetWithChain,
        hostContext: ComponentHostContext
    ): StakeActionsComponent = RelaychainStakeActionsComponent(
        stakingInteractor = stakingInteractor,
        resourceManager = resourceManager,
        router = router,
        stakeActionsValidations = stakeActionsValidations,
        assetWithChain = assetWithChain,
        hostContext = hostContext
    )
}

private class RelaychainStakeActionsComponent(
    private val stakingInteractor: StakingInteractor,
    private val resourceManager: ResourceManager,
    private val router: StakingRouter,
    private val stakeActionsValidations: Map<String, StakeActionsValidationSystem>,

    private val hostContext: ComponentHostContext,
    private val assetWithChain: SingleAssetSharedState.AssetWithChain,
) : StakeActionsComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<StakeActionsEvent>>()

    private val selectedAccountStakingStateFlow = hostContext.selectedAccount.flatMapLatest {
        stakingInteractor.selectedAccountStakingStateFlow(it, assetWithChain)
    }.shareInBackground()

    override val state: Flow<StakeActionsState?> = selectedAccountStakingStateFlow.transformLatest { stakingState ->
        if (stakingState is StakingState.Stash) {
            emit(StakeActionsState(availableActionsFor(stakingState)))
        } else {
            emit(null)
        }
    }
        .shareInBackground()

    override fun onAction(action: StakeActionsAction) {
        when (action) {
            is StakeActionsAction.ActionClicked -> manageStakeActionChosen(action.action)
        }
    }

    private fun manageStakeActionChosen(manageStakeAction: ManageStakeAction) {
        val validationSystem = stakeActionsValidations[manageStakeAction.id]

        if (validationSystem != null) {
            launch {
                val stakingState = selectedAccountStakingStateFlow.filterIsInstance<StakingState.Stash>().first()
                val payload = StakeActionsValidationPayload(stakingState)

                hostContext.validationExecutor.requireValid(
                    validationSystem = validationSystem,
                    payload = payload,
                    errorDisplayer = hostContext.errorDisplayer,
                    validationFailureTransformerDefault = { mainStakingValidationFailure(it, resourceManager) },
                ) {
                    navigateToAction(manageStakeAction)
                }
            }
        } else {
            navigateToAction(manageStakeAction)
        }
    }

    private fun navigateToAction(action: ManageStakeAction) {
        when (action.id) {
            SYSTEM_MANAGE_PAYOUTS -> router.openPayouts()
            SYSTEM_MANAGE_STAKING_BOND_MORE -> router.openBondMore()
            SYSTEM_MANAGE_STAKING_UNBOND -> router.openSelectUnbond()
            SYSTEM_MANAGE_CONTROLLER -> router.openControllerAccount()
            SYSTEM_MANAGE_VALIDATORS -> router.openCurrentValidators()
            SYSTEM_MANAGE_REWARD_DESTINATION -> router.openChangeRewardDestination()
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun availableActionsFor(stakingState: StakingState.Stash): List<ManageStakeAction> = buildList {
        add(ManageStakeAction.Companion::bondMore)
        add(ManageStakeAction.Companion::unbond)
        add(ManageStakeAction.Companion::rewardDestination)
        add(ManageStakeAction.Companion::controller)

        if (stakingState !is StakingState.Stash.None) {
            add(ManageStakeAction.Companion::payouts)
        }

        if (stakingState !is StakingState.Stash.Validator) {
            add(ManageStakeAction.Companion::validators)
        }
    }.map { it.invoke(resourceManager) }
}
