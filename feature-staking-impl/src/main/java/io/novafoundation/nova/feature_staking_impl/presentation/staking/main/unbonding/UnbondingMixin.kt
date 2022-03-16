package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.unbonding

import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.unbonding.rebond.RebondKind
import kotlinx.coroutines.flow.Flow

interface UnbondingMixin {

    sealed class State {

        object Empty : State()

        class HaveUnbondings(val redeemEnabled: Boolean, val unbondings: List<UnbondingModel>) : State()
    }

    val rebondKindAwaitable: ActionAwaitableMixin<Unit, RebondKind>

    val state: Flow<State>

    fun redeemClicked()

    fun cancelClicked()

    interface Presentation : UnbondingMixin
}
