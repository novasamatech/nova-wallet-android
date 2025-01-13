package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.yourPool

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.letOrHide

fun BaseFragment<*, *>.setupYourPoolComponent(component: YourPoolComponent, view: YourPoolView) {
    component.state.observe { optionalStakeSummaryState ->
        view.letOrHide(optionalStakeSummaryState, view::showYourPoolState)
    }
}
