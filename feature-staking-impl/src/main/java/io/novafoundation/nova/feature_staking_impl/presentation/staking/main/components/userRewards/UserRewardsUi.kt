package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view.UserRewardsView

fun BaseFragment<*>.setupUserRewardsComponent(component: UserRewardsComponent, view: UserRewardsView, router: StakingRouter) {
    view.setOnRewardPeriodClickedListener {
        component.onAction(UserRewardsAction.UserRewardPeriodClicked)
    }

    component.state.observe { userRewardsState ->
        view.setVisible(userRewardsState != null)

        when (userRewardsState) {
            is LoadingState.Loaded -> view.showValue(userRewardsState.data)
            is LoadingState.Loading -> view.showLoading()
            null -> {}
        }
    }

    component.events.observeEvent {
        when (it) {
            is UserRewardsEvent.UserRewardPeriodClicked -> router.openStakingPeriods()
        }
    }
}
