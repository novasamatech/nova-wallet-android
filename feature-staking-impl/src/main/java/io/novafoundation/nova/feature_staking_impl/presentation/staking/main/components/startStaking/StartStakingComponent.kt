package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.StakingRewardEstimationBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.UnsupportedComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.startStaking.relaychain.RelaychainStartStakingComponentFactory

typealias StartStakingComponent = StatefullComponent<StartStakingState, StartStakingEvent, StartStakingAction>

class StartStakingState(
    val estimateEarningsTitle: String,
    val returns: LoadingState<ReturnsModel>
)

class ReturnsModel(
    val monthlyPercentage: String,
    val yearlyPercentage: String,
)

sealed class StartStakingEvent {

    class ShowRewardEstimationDetails(val payload: StakingRewardEstimationBottomSheet.Payload) : StartStakingEvent()
}

sealed class StartStakingAction {

    object InfoClicked : StartStakingAction()

    object NextClicked : StartStakingAction()
}

class StartStakingComponentFactory(
    private val relaychainComponentFactory: RelaychainStartStakingComponentFactory,
    private val compoundStakingComponentFactory: CompoundStakingComponentFactory,
) {

    fun create(
        hostContext: ComponentHostContext
    ): StartStakingComponent = compoundStakingComponentFactory.create(
        relaychainComponentCreator = relaychainComponentFactory::create,
        parachainComponentCreator = { _, _ -> UnsupportedComponent() },
        hostContext = hostContext
    )
}
