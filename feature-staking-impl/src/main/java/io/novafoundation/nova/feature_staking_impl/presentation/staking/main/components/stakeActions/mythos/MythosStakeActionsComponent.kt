package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.mythos

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.presentation.dataOrNull
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.hasStakedCollators
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_BOND_MORE
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_STAKING_UNBOND
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.SYSTEM_MANAGE_VALIDATORS
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.mythos.loadUserStakeState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.ManageStakeAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.StakeActionsState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.bondMore
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.parachain.collators
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.unbond
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MythosStakeActionsComponentFactory(
    private val mythosSharedComputation: MythosSharedComputation,
    private val resourceManager: ResourceManager,
    private val router: MythosStakingRouter,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): StakeActionsComponent = MythosStakeActionsComponent(
        mythosSharedComputation = mythosSharedComputation,
        resourceManager = resourceManager,
        router = router,
        stakingOption = stakingOption,
        hostContext = hostContext
    )
}

private class MythosStakeActionsComponent(
    private val mythosSharedComputation: MythosSharedComputation,
    private val resourceManager: ResourceManager,
    private val router: MythosStakingRouter,

    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
) : StakeActionsComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<StakeActionsEvent>>()

    override val state = mythosSharedComputation.loadUserStakeState(
        hostContext = hostContext,
        stateProducer = ::stateFor
    )
        .map { it?.dataOrNull }
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
            SYSTEM_MANAGE_STAKING_BOND_MORE -> router.openBondMore()
            SYSTEM_MANAGE_STAKING_UNBOND -> router.openUnbond()
            SYSTEM_MANAGE_VALIDATORS -> router.openStakedCollators()
        }
    }

    private fun stateFor(delegatorState: MythosDelegatorState.Staked): Flow<StakeActionsState> {
        return flowOf {
            val availableActions = availableStakingActionsFor(delegatorState)

            StakeActionsState(availableActions)
        }
    }

    private fun availableStakingActionsFor(delegatorState: MythosDelegatorState.Staked): List<ManageStakeAction> = buildList {
        add(ManageStakeAction.bondMore(resourceManager))

        if (delegatorState.hasStakedCollators()) {
            add(ManageStakeAction.unbond(resourceManager))
        }

        val collatorsCount = delegatorState.userStakeInfo.candidates.size.format()
        add(ManageStakeAction.collators(resourceManager, collatorsCount))
    }
}
