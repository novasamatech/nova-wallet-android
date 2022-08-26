package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.parachain

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary.ParachainStakingUnbondPreliminaryValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.unbond.validations.preliminary.ParachainStakingUnbondPreliminaryValidationSystem
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_BOND_MORE
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_UNBOND
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_VALIDATORS
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.common.validation.unbondPreliminaryValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.parachainStaking.loadDelegatingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.ManageStakeAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.bondMore
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.unbond
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ParachainStakeActionsComponentFactory(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val resourceManager: ResourceManager,
    private val router: ParachainStakingRouter,
    private val validationExecutor: ValidationExecutor,
    private val unbondPreliminaryValidationSystem: ParachainStakingUnbondPreliminaryValidationSystem,
) {

    fun create(
        assetWithChain: SingleAssetSharedState.AssetWithChain,
        hostContext: ComponentHostContext
    ): StakeActionsComponent = ParachainStakeActionsComponent(
        delegatorStateUseCase = delegatorStateUseCase,
        resourceManager = resourceManager,
        assetWithChain = assetWithChain,
        hostContext = hostContext,
        router = router,
        validationExecutor = validationExecutor,
        unbondValidationSystem = unbondPreliminaryValidationSystem
    )
}

private class ParachainStakeActionsComponent(
    delegatorStateUseCase: DelegatorStateUseCase,
    private val resourceManager: ResourceManager,
    private val router: ParachainStakingRouter,

    private val assetWithChain: SingleAssetSharedState.AssetWithChain,
    private val hostContext: ComponentHostContext,

    private val unbondValidationSystem: ParachainStakingUnbondPreliminaryValidationSystem,
    private val validationExecutor: ValidationExecutor
) : StakeActionsComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<StakeActionsEvent>>()

    override val state = delegatorStateUseCase.loadDelegatingState(
        hostContext = hostContext,
        assetWithChain = assetWithChain,
        stateProducer = ::stateFor
    )
        .map { it?.dataOrNull } // we don't need loading state in this component
        .shareInBackground()

    override fun onAction(action: StakeActionsAction) {
        when (action) {
            is StakeActionsAction.ActionClicked -> {
                navigateToAction(action.action)
            }
        }
    }

    private fun navigateToAction(action: ManageStakeAction) {
        when (action.id) {
            SYSTEM_MANAGE_VALIDATORS -> router.openCurrentCollators()
            SYSTEM_MANAGE_STAKING_BOND_MORE -> router.openStartStaking()
            SYSTEM_MANAGE_STAKING_UNBOND -> openUnbondIfValid()
        }
    }

    private fun stateFor(delegatorState: DelegatorState.Delegator): Flow<StakeActionsState> {
        return flowOf {
            val availableActions = availableActionsFor(delegatorState)

            StakeActionsState(availableActions)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun availableActionsFor(delegatorState: DelegatorState.Delegator): List<ManageStakeAction> = buildList {
        add(ManageStakeAction.bondMore(resourceManager))
        add(ManageStakeAction.unbond(resourceManager))

        val collatorsCount = delegatorState.delegations.size.format()
        add(ManageStakeAction.collators(resourceManager, collatorsCount))
    }

    private fun openUnbondIfValid() = launch {
        validationExecutor.requireValid(
            validationSystem = unbondValidationSystem,
            payload = ParachainStakingUnbondPreliminaryValidationPayload,
            errorDisplayer = hostContext.errorDisplayer,
            validationFailureTransformerDefault = { unbondPreliminaryValidationFailure(it, resourceManager) },
        ) {
            router.openUnbond()
        }
    }
}

fun ManageStakeAction.Companion.collators(resourceManager: ResourceManager, collatorsCount: String): ManageStakeAction {
    return ManageStakeAction(
        id = SYSTEM_MANAGE_VALIDATORS,
        label = resourceManager.getString(R.string.staking_parachain_your_collators),
        iconRes = R.drawable.ic_validators_outline,
        badge = collatorsCount
    )
}
