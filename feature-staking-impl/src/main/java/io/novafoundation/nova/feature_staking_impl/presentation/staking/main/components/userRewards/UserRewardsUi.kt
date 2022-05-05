package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.userRewards

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view.UserRewardsView

fun BaseFragment<*>.setupUserRewardsComponent(component: UserRewardsComponent, view: UserRewardsView) {
    component.state.observe { userRewardsState ->
        view.setVisible(userRewardsState != null)

        when (userRewardsState) {
            is LoadingState.Loaded -> view.showValue(userRewardsState.data)
            is LoadingState.Loading -> view.showLoading()
        }
    }
}
