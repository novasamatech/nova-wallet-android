package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.nominationPools

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.shareInBackground
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.StakingOption
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolMember
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.NominationPoolSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.alerts.NominationPoolAlert
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.main.alerts.NominationPoolsAlertsInteractor
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.common.nominationPools.loadPoolMemberState
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class NominationPoolsAlertsComponentFactory(
    private val nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val interactor: NominationPoolsAlertsInteractor,
    private val resourceManager: ResourceManager,
) {

    fun create(
        stakingOption: StakingOption,
        hostContext: ComponentHostContext
    ): AlertsComponent = NominationPoolsAlertsComponent(
        resourceManager = resourceManager,
        stakingOption = stakingOption,
        hostContext = hostContext,
        nominationPoolSharedComputation = nominationPoolSharedComputation,
        interactor = interactor
    )
}

private open class NominationPoolsAlertsComponent(
    private val interactor: NominationPoolsAlertsInteractor,
    private val resourceManager: ResourceManager,
    nominationPoolSharedComputation: NominationPoolSharedComputation,
    private val hostContext: ComponentHostContext,
    private val stakingOption: StakingOption,
) : AlertsComponent, CoroutineScope by hostContext.scope {

    override val events = MutableLiveData<Event<AlertsEvent>>()

    override val state = nominationPoolSharedComputation.loadPoolMemberState(
        hostContext = hostContext,
        chain = stakingOption.assetWithChain.chain,
        stateProducer = ::constructAlertsState,
        distinctUntilChanged = { old, new -> old?.poolId == new?.poolId }
    )
        .shareInBackground()

    override fun onAction(action: AlertsAction) {}

    private fun constructAlertsState(poolMember: PoolMember): Flow<List<AlertModel>> {
        return combine(
            interactor.alertsFlow(poolMember, stakingOption.assetWithChain.chain, hostContext.scope),
            hostContext.assetFlow,
        ) { alerts, asset ->
            alerts.map { mapAlertToAlertModel(it, asset) }
        }
    }

    private fun openRedeem() {
        // TODO redeem flow
    }

    private fun mapAlertToAlertModel(alert: NominationPoolAlert, asset: Asset): AlertModel {
        return when (alert) {
            is NominationPoolAlert.RedeemTokens -> {
                val amount = mapAmountToAmountModel(alert.amount, asset).token

                AlertModel(
                    title = resourceManager.getString(R.string.staking_alert_redeem_title),
                    extraMessage = amount,
                    type = AlertModel.Type.CallToAction(::openRedeem)
                )
            }

            NominationPoolAlert.WaitingForNextEra -> AlertModel(
                resourceManager.getString(R.string.staking_nominator_status_alert_waiting_message),
                resourceManager.getString(R.string.staking_alert_start_next_era_message),
                AlertModel.Type.Info
            )
        }
    }
}
