package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.stakeSummary

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.dialog.infoDialog

fun BaseFragment<*>.setupStakeSummaryComponent(component: StakeSummaryComponent, view: StakeSummaryView) {
    // state
    component.state.observe { stakeSummaryState ->
        if (stakeSummaryState == null) {
            view.makeGone()
            return@observe
        }

        view.makeVisible()

        when (stakeSummaryState) {
            is LoadingState.Loaded -> {
                val summary = stakeSummaryState.data

                view.showStakeAmount(summary.totalStaked)
                view.showStakeStatus(mapStatus(summary.status))
            }
            is LoadingState.Loading -> view.showLoading()
        }
    }
}

private fun mapStatus(status: StakeStatusModel): StakeSummaryView.Status {
    return when (status) {
        is StakeStatusModel.Active -> StakeSummaryView.Status.Active
        is StakeStatusModel.Inactive -> StakeSummaryView.Status.Inactive
        is StakeStatusModel.Waiting -> StakeSummaryView.Status.Waiting(status.timeLeft, status.messageFormat)
    }
}
