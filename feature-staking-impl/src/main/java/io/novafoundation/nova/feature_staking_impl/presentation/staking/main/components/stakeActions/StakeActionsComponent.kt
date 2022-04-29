package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions

import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.UnsupportedComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions.relaychain.RelaychainStakeActionsComponentFactory

typealias StakeActionsComponent = StatefullComponent<StakeActionsState, StakeActionsEvent, StakeActionsAction>

class StakeActionsState(
    val availableActions: List<ManageStakeAction>
)

typealias StakeActionsEvent = Nothing

sealed class StakeActionsAction {

    class ActionClicked(val action: ManageStakeAction) : StakeActionsAction()
}

class StakeActionsComponentFactory(
    private val relaychainComponentFactory: RelaychainStakeActionsComponentFactory,
    private val compoundStakingComponentFactory: CompoundStakingComponentFactory,
) {

    fun create(
        hostContext: ComponentHostContext
    ): StakeActionsComponent = compoundStakingComponentFactory.create(
        relaychainComponentCreator = relaychainComponentFactory::create,
        parachainComponentCreator = { _, _ -> UnsupportedComponent() },
        hostContext = hostContext
    )
}
