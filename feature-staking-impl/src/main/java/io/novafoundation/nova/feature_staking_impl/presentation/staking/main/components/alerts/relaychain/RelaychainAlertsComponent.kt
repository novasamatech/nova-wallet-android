package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.relaychain

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.WithCoroutineScopeExtensions
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.alerts.Alert
import io.novafoundation.nova.feature_staking_impl.domain.alerts.Alert.ChangeValidators.Reason
import io.novafoundation.nova.feature_staking_impl.domain.alerts.AlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.main.StakeActionsValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.ComponentHostContext
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsAction
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsComponent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsEvent
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.components.alerts.AlertsState
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.mainStakingValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.math.BigDecimal

class RelaychainAlertsComponentFactory(
    private val stakingInteractor: StakingInteractor,
    private val alertsInteractor: AlertsInteractor,
    private val resourceManager: ResourceManager,
    private val redeemValidationSystem: StakeActionsValidationSystem,
    private val bondMoreValidationSystem: StakeActionsValidationSystem,
    private val router: StakingRouter,
) {

    fun create(
        assetWithChain: SingleAssetSharedState.AssetWithChain,
        hostContext: ComponentHostContext,
    ): AlertsComponent = RelaychainAlertsComponent(
        stakingInteractor = stakingInteractor,
        resourceManager = resourceManager,
        alertsInteractor = alertsInteractor,
        redeemValidationSystem = redeemValidationSystem,
        bondMoreValidationSystem = bondMoreValidationSystem,
        router = router,

        assetWithChain = assetWithChain,
        hostContext = hostContext
    )
}

private class RelaychainAlertsComponent(
    private val stakingInteractor: StakingInteractor,
    private val alertsInteractor: AlertsInteractor,
    private val resourceManager: ResourceManager,

    private val hostContext: ComponentHostContext,
    private val assetWithChain: SingleAssetSharedState.AssetWithChain,

    private val redeemValidationSystem: StakeActionsValidationSystem,
    private val bondMoreValidationSystem: StakeActionsValidationSystem,
    private val router: StakingRouter,
) : AlertsComponent,
    CoroutineScope by hostContext.scope,
    WithCoroutineScopeExtensions by WithCoroutineScopeExtensions(hostContext.scope) {

    private val selectedAccountStakingStateFlow = hostContext.selectedAccount.flatMapLatest {
        stakingInteractor.selectedAccountStakingStateFlow(it, assetWithChain)
    }.shareInBackground()

    override val events = MutableLiveData<Event<AlertsEvent>>()

    override val state: Flow<AlertsState?> = selectedAccountStakingStateFlow.flatMapLatest {
        alertsInteractor.getAlertsFlow(it)
    }
        .mapList { mapAlertToAlertModel(it) }
        .withLoading()
        .onStart<AlertsState?> { emit(null) }
        .shareInBackground()

    override fun onAction(action: AlertsAction) {}

    private fun mapAlertToAlertModel(alert: Alert): AlertModel {
        return when (alert) {
            is Alert.ChangeValidators -> {
                val message = when (alert.reason) {
                    Reason.NONE_ELECTED -> R.string.staking_nominator_status_alert_no_validators
                    Reason.OVERSUBSCRIBED -> R.string.staking_your_oversubscribed_message
                }

                AlertModel(
                    resourceManager.getString(R.string.staking_alert_change_validators),
                    resourceManager.getString(message),
                    AlertModel.Type.CallToAction { router.openCurrentValidators() }
                )
            }

            is Alert.RedeemTokens -> {
                AlertModel(
                    resourceManager.getString(R.string.staking_alert_redeem_title),
                    formatAlertTokenAmount(alert.amount, alert.token),
                    AlertModel.Type.CallToAction(::redeemAlertClicked)
                )
            }

            is Alert.BondMoreTokens -> {
                val existentialDepositDisplay = formatAlertTokenAmount(alert.minimalStake, alert.token)

                AlertModel(
                    resourceManager.getString(R.string.staking_alert_bond_more_title),
                    resourceManager.getString(R.string.staking_alert_bond_more_message, existentialDepositDisplay),
                    AlertModel.Type.CallToAction(::bondMoreAlertClicked)
                )
            }

            is Alert.WaitingForNextEra -> AlertModel(
                resourceManager.getString(R.string.staking_nominator_status_alert_waiting_message),
                resourceManager.getString(R.string.staking_alert_start_next_era_message),
                AlertModel.Type.Info
            )
            Alert.SetValidators -> AlertModel(
                resourceManager.getString(R.string.staking_set_validators_title),
                resourceManager.getString(R.string.staking_set_validators_message),
                AlertModel.Type.CallToAction { router.openCurrentValidators() }
            )
        }
    }

    private fun formatAlertTokenAmount(amount: BigDecimal, token: Token): String {
        val formattedFiat = token.priceOf(amount).formatAsCurrency()
        val formattedAmount = amount.formatTokenAmount(token.configuration)

        return buildString {
            append(formattedAmount)

            formattedFiat.let {
                append(" ($it)")
            }
        }
    }

    private fun bondMoreAlertClicked() = requireValidManageStakingAction(bondMoreValidationSystem) {
        router.openBondMore()
    }

    private fun redeemAlertClicked() = requireValidManageStakingAction(redeemValidationSystem) {
        router.openRedeem()
    }

    private fun requireValidManageStakingAction(
        validationSystem: StakeActionsValidationSystem,
        action: () -> Unit,
    ) = launch {
        val stakingState = selectedAccountStakingStateFlow.first()
        val stashState = stakingState as? StakingState.Stash ?: return@launch

        hostContext.validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = StakeActionsValidationPayload(stashState),
            errorDisplayer = hostContext.errorDisplayer,
            validationFailureTransformerDefault = { mainStakingValidationFailure(it, resourceManager) },
        ) {
            action()
        }
    }
}
