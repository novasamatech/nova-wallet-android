package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.ChooseOneOfAwaitableAction
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.Identifiable
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.ChooseStakedStakeTargetsBottomSheet
import io.novafoundation.nova.feature_staking_impl.presentation.common.selectStakeTarget.SelectStakeTargetModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.AwaitableEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ChooseOneOfAwaitableEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.UnsupportedComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.nominationPools.NominationPoolsUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.parachain.ParachainUnbondingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.rebond.RebondKind
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.relaychain.RelaychainUnbondingComponentFactory

typealias UnbondingComponent = StatefullComponent<LoadingState<UnbondingState>, UnbondingEvent, UnbondingAction>
typealias ChooseOneOfStakedTargetsAction<E> = ActionAwaitableMixin.Action<ChooseStakedStakeTargetsBottomSheet.Payload<E>, E>
typealias ChooseOneOfStakedTargetsEvent<E> = AwaitableEvent<ChooseStakedStakeTargetsBottomSheet.Payload<E>, E>

sealed class UnbondingState {

    companion object

    object Empty : UnbondingState()

    data class HaveUnbondings(
        val redeemEnabled: Boolean,
        val cancelState: ButtonState,
        val unbondings: List<UnbondingModel>
    ) : UnbondingState()
}

sealed class UnbondingEvent {

    class ChooseRebondKind(
        override val value: ChooseOneOfAwaitableAction<RebondKind>
    ) : ChooseOneOfAwaitableEvent<RebondKind>, UnbondingEvent()

    class ChooseRebondTarget(
        override val value: ChooseOneOfStakedTargetsAction<SelectStakeTargetModel<Identifiable>>
    ) : ChooseOneOfStakedTargetsEvent<SelectStakeTargetModel<Identifiable>>, UnbondingEvent()
}

sealed class UnbondingAction {

    object RebondClicked : UnbondingAction()

    object RedeemClicked : UnbondingAction()
}

class UnbondingComponentFactory(
    private val relaychainUnbondingComponentFactory: RelaychainUnbondingComponentFactory,
    private val parachainComponentFactory: ParachainUnbondingComponentFactory,
    private val nominationPoolsUnbondingComponentFactory: NominationPoolsUnbondingComponentFactory,
    private val compoundStakingComponentFactory: CompoundStakingComponentFactory,
) {

    fun create(
        hostContext: ComponentHostContext
    ): UnbondingComponent = compoundStakingComponentFactory.create(
        relaychainComponentCreator = relaychainUnbondingComponentFactory::create,
        parachainComponentCreator = parachainComponentFactory::create,
        nominationPoolsCreator = nominationPoolsUnbondingComponentFactory::create,
        // TODO unbonding
        mythosCreator = UnsupportedComponent.creator(),
        hostContext = hostContext
    )
}
