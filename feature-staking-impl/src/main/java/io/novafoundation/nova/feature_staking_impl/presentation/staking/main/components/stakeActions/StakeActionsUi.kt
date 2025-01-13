package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeActions

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view.ManageStakingView

fun BaseFragment<*, *>.setupStakeActionsComponent(component: StakeActionsComponent, view: ManageStakingView) {
    // state
    component.state.observe { stakeActionsState ->
        if (stakeActionsState == null) {
            view.makeGone()
            return@observe
        }

        view.makeVisible()

        view.setAvailableActions(stakeActionsState.availableActions)
    }

    // actions
    view.onManageStakeActionClicked {
        component.onAction(StakeActionsAction.ActionClicked(it))
    }
}
