package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.networkInfo

import io.novafoundation.nova.common.base.BaseFragment

fun BaseFragment<*, *>.setupNetworkInfoComponent(component: NetworkInfoComponent, view: NetworkInfoView) {
    // state
    component.state.observe { networkInfoState ->
        view.setState(networkInfoState)
    }

    // actions
    view.onExpandClicked {
        component.onAction(NetworkInfoAction.ChangeExpendedStateClicked)
    }
}
