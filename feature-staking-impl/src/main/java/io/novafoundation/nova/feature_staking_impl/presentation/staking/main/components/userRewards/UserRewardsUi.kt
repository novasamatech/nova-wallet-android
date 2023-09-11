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

    view.setClaimClickListener {
        component.onAction(UserRewardsAction.ClaimRewardsClicked)
    }

    component.state.observe { userRewardsState ->
        view.setVisible(userRewardsState != null)

        userRewardsState?.selectedRewardPeriod?.let { view.setStakingPeriod(it) }

        when (val amount = userRewardsState?.amount) {
            is LoadingState.Loaded -> view.showRewards(amount.data)
            is LoadingState.Loading -> view.showPendingRewardsLoading()
            null -> {}
        }

        view.setClaimableRewardsState(userRewardsState?.claimableRewards)
        userRewardsState?.iconRes?.let { view.setBannerImage(it) }
    }

    component.events.observeEvent {
        when (it) {
            is UserRewardsEvent.UserRewardPeriodClicked -> router.openStakingPeriods()
        }
    }
}
