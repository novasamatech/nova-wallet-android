package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts

import io.novafoundation.nova.common.base.BaseFragment

fun BaseFragment<*, *>.setupAlertsComponent(component: AlertsComponent, view: AlertsView) {
    // state
    component.state.observe { networkInfoState ->
        view.setState(networkInfoState)
    }
}
