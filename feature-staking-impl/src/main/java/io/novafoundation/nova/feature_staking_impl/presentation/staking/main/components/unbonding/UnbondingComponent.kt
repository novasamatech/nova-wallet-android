package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding

import io.novafoundation.nova.common.mixin.actionAwaitable.ChooseOneOfAwaitableAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ChooseOneOfAwaitableEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.UnsupportedComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.rebond.RebondKind
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.relaychain.RelaychainUnbondingComponentFactory

typealias UnbondingComponent = StatefullComponent<UnbondingState, UnbondingEvent, UnbondingAction>

sealed class UnbondingState {

    object Empty : UnbondingState()

    class HaveUnbondings(
        val redeemEnabled: Boolean,
        val cancelEnabled: Boolean,
        val unbondings: List<UnbondingModel>
    ) : UnbondingState()
}

sealed class UnbondingEvent {

    class ChooseRebondKind(
        override val value: ChooseOneOfAwaitableAction<RebondKind>
    ) : ChooseOneOfAwaitableEvent<RebondKind>, UnbondingEvent()
}

sealed class UnbondingAction {

    object RebondClicked : UnbondingAction()

    object RedeemClicked : UnbondingAction()
}

class UnbondingComponentFactory(
    private val relaychainUnbondingComponentFactory: RelaychainUnbondingComponentFactory,
    private val compoundStakingComponentFactory: CompoundStakingComponentFactory,
) {

    fun create(
        hostContext: ComponentHostContext
    ): UnbondingComponent = compoundStakingComponentFactory.create(
        relaychainComponentCreator = relaychainUnbondingComponentFactory::create,
        parachainComponentCreator = { _, _ -> UnsupportedComponent() },
        hostContext = hostContext
    )
}
