package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.unbonding.rebond.ChooseRebondKindBottomSheet

fun BaseFragment<*>.setupUnbondingComponent(component: UnbondingComponent, view: UnbondingsView) {
    component.events.observeEvent {
        when (it) {
            is UnbondingEvent.ChooseRebondKind -> {
                ChooseRebondKindBottomSheet(requireContext(), it.value)
                    .show()
            }
        }
    }

    view.onCancelClicked { component.onAction(UnbondingAction.RebondClicked) }
    view.onRedeemClicked { component.onAction(UnbondingAction.RedeemClicked) }

    component.state.observe { state ->
        if (state == null) {
            view.makeGone()
        } else {
            view.makeVisible()
            view.setState(state)
        }
    }
}
