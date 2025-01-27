package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts

import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.CompoundStakingComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.StatefullComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.UnsupportedComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.nominationPools.NominationPoolsAlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.parachain.ParachainAlertsComponentFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.relaychain.RelaychainAlertsComponentFactory

typealias AlertsComponent = StatefullComponent<AlertsState, AlertsEvent, AlertsAction>

typealias AlertsState = LoadingState<List<AlertModel>>

typealias AlertsEvent = Nothing

typealias AlertsAction = Nothing

class AlertsComponentFactory(
    private val relaychainComponentFactory: RelaychainAlertsComponentFactory,
    private val parachainAlertsComponentFactory: ParachainAlertsComponentFactory,
    private val nominationPoolsFactory: NominationPoolsAlertsComponentFactory,
    private val compoundStakingComponentFactory: CompoundStakingComponentFactory,
) {

    fun create(
        hostContext: ComponentHostContext,
    ): AlertsComponent = compoundStakingComponentFactory.create(
        relaychainComponentCreator = relaychainComponentFactory::create,
        parachainComponentCreator = parachainAlertsComponentFactory::create,
        nominationPoolsCreator = nominationPoolsFactory::create,
        // TODO alerts
        mythosCreator = UnsupportedComponent.creator(),
        hostContext = hostContext
    )
}
