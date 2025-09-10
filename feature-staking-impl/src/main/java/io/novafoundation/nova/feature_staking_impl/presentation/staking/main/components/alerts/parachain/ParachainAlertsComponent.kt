package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.parachain

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.alerts.ParachainStakingAlert
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.main.alerts.ParachainStakingAlertsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.parachainStaking.loadDelegatingState
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ParachainAlertsComponentFactory(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val interactor: ParachainStakingAlertsInteractor,
    private val resourceManager: ResourceManager,
    private val router: ParachainStakingRouter,
    private val amountFormatter: AmountFormatter
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext,
    ): AlertsComponent = ParachainAlertsComponent(
        delegatorStateUseCase = delegatorStateUseCase,
        interactor = interactor,
        resourceManager = resourceManager,
        stakingOption = stakingOption,
        hostContext = hostContext,
        router = router,
        amountFormatter = amountFormatter
    )
}

private class ParachainAlertsComponent(
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val interactor: ParachainStakingAlertsInteractor,
    private val resourceManager: ResourceManager,
    private val amountFormatter: AmountFormatter,

    private val stakingOption: StakingOption,
    private val hostContext: ComponentHostContext,
    private val router: ParachainStakingRouter
) : AlertsComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    override val events = MutableLiveData<Event<AlertsEvent>>()

    override val state = delegatorStateUseCase.loadDelegatingState(
        hostContext = hostContext,
        assetWithChain = stakingOption.assetWithChain,
        stateProducer = ::stateFor
    )
        .shareInBackground()

    override fun onAction(action: AlertsAction) {}

    private fun stateFor(delegatorState: DelegatorState.Delegator): Flow<List<AlertModel>> {
        return combine(
            interactor.alertsFlow(delegatorState),
            hostContext.assetFlow,
        ) { alerts, asset ->
            alerts.map { mapAlertToAlertModel(it, asset) }
        }
    }

    private fun mapAlertToAlertModel(alert: ParachainStakingAlert, asset: Asset): AlertModel {
        return when (alert) {
            ParachainStakingAlert.ChangeCollator -> AlertModel(
                title = resourceManager.getString(R.string.parachain_staking_change_collator),
                extraMessage = resourceManager.getString(R.string.parachain_staking_alerts_change_collator_message),
                type = AlertModel.Type.CallToAction { router.openCurrentCollators() }
            )

            is ParachainStakingAlert.RedeemTokens -> {
                val amount = amountFormatter.formatAmountToAmountModel(alert.redeemableAmount, asset).token

                AlertModel(
                    title = resourceManager.getString(R.string.staking_alert_redeem_title),
                    extraMessage = resourceManager.getString(R.string.parachain_staking_alerts_redeem_message, amount),
                    type = AlertModel.Type.CallToAction { router.openRedeem() }
                )
            }

            ParachainStakingAlert.StakeMore -> AlertModel(
                title = resourceManager.getString(R.string.staking_bond_more_v1_9_0),
                extraMessage = resourceManager.getString(R.string.parachain_staking_alerts_bond_more_message),
                type = AlertModel.Type.CallToAction { router.openCurrentCollators() }
            )
        }
    }
}
