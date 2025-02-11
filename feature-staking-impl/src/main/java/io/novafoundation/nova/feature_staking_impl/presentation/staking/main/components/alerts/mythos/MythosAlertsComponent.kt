package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.mythos

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.data.memory.ComputationalScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.model.MythosDelegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.alerts.MythosStakingAlert
import io.novafoundation.nova.feature_staking_impl.domain.mythos.main.alerts.MythosStakingAlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.alerts.ParachainStakingAlert
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.alerts.ParachainStakingAlertsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.mythos.loadUserStakeState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.parachainStaking.loadDelegatingState
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class MythosAlertsComponentFactory(
    private val mythosSharedComputation: MythosSharedComputation,
    private val interactor: MythosStakingAlertsInteractor,
    private val resourceManager: ResourceManager,
    private val router: MythosStakingRouter
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext,
    ): AlertsComponent = MythosAlertsComponent(
        mythosSharedComputation = mythosSharedComputation,
        interactor = interactor,
        resourceManager = resourceManager,
        hostContext = hostContext,
        router = router
    )
}

private class MythosAlertsComponent(
    private val mythosSharedComputation: MythosSharedComputation,
    private val interactor: MythosStakingAlertsInteractor,
    private val resourceManager: ResourceManager,
    private val hostContext: ComponentHostContext,
    private val router: MythosStakingRouter
) : AlertsComponent,
    ComputationalScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<AlertsEvent>>()

    override val state = mythosSharedComputation.loadUserStakeState(
        hostContext = hostContext,
        stateProducer = ::stateFor
    )
        .shareInBackground()

    override fun onAction(action: AlertsAction) {}

    private fun stateFor(delegatorState: MythosDelegatorState.Staked): Flow<List<AlertModel>> {
        return combine(
            interactor.alertsFlow(delegatorState),
            hostContext.assetFlow,
        ) { alerts, asset ->
            alerts.map { mapAlertToAlertModel(it, asset) }
        }
    }

    private fun mapAlertToAlertModel(alert: MythosStakingAlert, asset: Asset): AlertModel {
        return when (alert) {
            MythosStakingAlert.ChangeCollator -> AlertModel(
                title = resourceManager.getString(R.string.parachain_staking_change_collator),
                extraMessage = resourceManager.getString(R.string.parachain_staking_alerts_change_collator_message),
                type = AlertModel.Type.CallToAction { router.openStakedCollators() }
            )

            is MythosStakingAlert.RedeemTokens -> {
                val amount = mapAmountToAmountModel(alert.redeemableAmount, asset).token

                AlertModel(
                    title = resourceManager.getString(R.string.staking_alert_redeem_title),
                    extraMessage = resourceManager.getString(R.string.parachain_staking_alerts_redeem_message, amount),
                    type = AlertModel.Type.CallToAction { router.openRedeem() }
                )
            }
        }
    }
}
