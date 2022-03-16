package io.novafoundation.nova.feature_staking_impl.presentation.staking.main

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.MixinFactory
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.LoadingState
import io.novafoundation.nova.common.presentation.flatMapLoading
import io.novafoundation.nova.common.presentation.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.childScope
import io.novafoundation.nova.common.utils.format
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.mapList
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.core.updater.UpdateSystem
import io.novafoundation.nova.feature_staking_api.domain.model.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.alerts.Alert
import io.novafoundation.nova.feature_staking_impl.domain.alerts.AlertsInteractor
import io.novafoundation.nova.feature_staking_impl.domain.model.NetworkInfo
import io.novafoundation.nova.feature_staking_impl.domain.model.StakingPeriod
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.ManageStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.balance.ManageStakingValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.alerts.model.AlertModel
import io.novafoundation.nova.feature_staking_impl.presentation.staking.balance.manageStakingActionValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.staking.bond.select.SelectBondMorePayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.di.StakingViewStateFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.StakingNetworkInfoModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.WithAssetSelector
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.SingleAssetSharedState
import io.novafoundation.nova.runtime.state.selectedChainFlow
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.math.BigDecimal

private const val CURRENT_ICON_SIZE = 40

private val WARNING_ICON = R.drawable.ic_warning_filled
private val WAITING_ICON = R.drawable.ic_time_24

class StakingViewModel(
    private val interactor: StakingInteractor,
    private val alertsInteractor: AlertsInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val stakingViewStateFactory: StakingViewStateFactory,
    private val router: StakingRouter,
    private val resourceManager: ResourceManager,
    private val redeemValidationSystem: ManageStakingValidationSystem,
    private val bondMoreValidationSystem: ManageStakingValidationSystem,
    private val validationExecutor: ValidationExecutor,
    stakingUpdateSystem: UpdateSystem,
    assetSelectorMixinFactory: MixinFactory<AssetSelectorMixin.Presentation>,
    selectedAssetState: SingleAssetSharedState,
) : BaseViewModel(),
    WithAssetSelector,
    Validatable by validationExecutor {

    override val assetSelectorMixin = assetSelectorMixinFactory.create(scope = this)

    private val stakingStateScope = viewModelScope.childScope(supervised = true)

    private val selectionState = interactor.selectionStateFlow()
        .share()

    private val loadingStakingState = selectionState
        .withLoading { (account, assetWithToken) ->
            interactor.selectedAccountStakingStateFlow(account, assetWithToken)
        }.share()

    val stakingViewStateFlow = loadingStakingState
        .onEach { stakingStateScope.coroutineContext.cancelChildren() }
        .mapLoading(::transformStakingState)
        .inBackground()
        .share()

    private val selectedChain = selectedAssetState.selectedChainFlow()
        .share()

    val networkInfoStateLiveData = selectedChain
        .distinctUntilChanged()
        .withLoading { chain ->
            interactor.observeNetworkInfoState(chain.id).combine(assetSelectorMixin.selectedAssetFlow) { networkInfo, asset ->
                transformNetworkInfo(asset, networkInfo)
            }
        }
        .inBackground()
        .asLiveData()

    val currentAddressModelLiveData = currentAddressModelFlow().asLiveData()

    val alertsFlow = loadingStakingState
        .flatMapLoading {
            alertsInteractor.getAlertsFlow(it)
                .mapList(::mapAlertToAlertModel)
        }
        .inBackground()
        .asLiveData()

    init {
        stakingUpdateSystem.start()
            .launchIn(this)
    }

    fun avatarClicked() {
        router.openChangeAccount()
    }

    private fun mapAlertToAlertModel(alert: Alert): AlertModel {
        return when (alert) {
            Alert.ChangeValidators -> {
                AlertModel(
                    WARNING_ICON,
                    resourceManager.getString(R.string.staking_alert_change_validators),
                    resourceManager.getString(R.string.staking_nominator_status_alert_no_validators),
                    AlertModel.Type.CallToAction { router.openCurrentValidators() }
                )
            }

            is Alert.RedeemTokens -> {
                AlertModel(
                    WARNING_ICON,
                    resourceManager.getString(R.string.staking_alert_redeem_title),
                    formatAlertTokenAmount(alert.amount, alert.token),
                    AlertModel.Type.CallToAction(::redeemAlertClicked)
                )
            }

            is Alert.BondMoreTokens -> {
                val existentialDepositDisplay = formatAlertTokenAmount(alert.minimalStake, alert.token)

                AlertModel(
                    WARNING_ICON,
                    resourceManager.getString(R.string.staking_alert_bond_more_title),
                    resourceManager.getString(R.string.staking_alert_bond_more_message, existentialDepositDisplay),
                    AlertModel.Type.CallToAction(::bondMoreAlertClicked)
                )
            }

            is Alert.WaitingForNextEra -> AlertModel(
                WAITING_ICON,
                resourceManager.getString(R.string.staking_nominator_status_alert_waiting_message),
                resourceManager.getString(R.string.staking_alert_start_next_era_message),
                AlertModel.Type.Info
            )
            Alert.SetValidators -> AlertModel(
                WARNING_ICON,
                resourceManager.getString(R.string.staking_set_validators_title),
                resourceManager.getString(R.string.staking_set_validators_message),
                AlertModel.Type.CallToAction { router.openCurrentValidators() }
            )
        }
    }

    private fun formatAlertTokenAmount(amount: BigDecimal, token: Token): String {
        val formattedFiat = token.fiatAmount(amount).formatAsCurrency()
        val formattedAmount = amount.formatTokenAmount(token.configuration)

        return buildString {
            append(formattedAmount)

            formattedFiat.let {
                append(" ($it)")
            }
        }
    }

    private fun bondMoreAlertClicked() = requireValidManageStakingAction(bondMoreValidationSystem) {
        val bondMorePayload = SelectBondMorePayload(overrideFinishAction = StakingRouter::returnToMain)

        router.openBondMore(bondMorePayload)
    }

    private fun redeemAlertClicked() = requireValidManageStakingAction(redeemValidationSystem) {
        router.openRedeem()
    }

    private fun requireValidManageStakingAction(
        validationSystem: ManageStakingValidationSystem,
        action: () -> Unit,
    ) = launch {
        val stakingState = (loadingStakingState.first() as? LoadingState.Loaded)?.data
        val stashState = stakingState as? StakingState.Stash ?: return@launch

        validationExecutor.requireValid(
            validationSystem,
            ManageStakingValidationPayload(stashState),
            validationFailureTransformer = { manageStakingActionValidationFailure(it, resourceManager) }
        ) {
            action()
        }
    }

    private fun transformStakingState(accountStakingState: StakingState) = when (accountStakingState) {
        is StakingState.Stash.Nominator -> stakingViewStateFactory.createNominatorViewState(
            accountStakingState,
            assetSelectorMixin.selectedAssetFlow,
            stakingStateScope,
            ::showError
        )

        is StakingState.Stash.None -> stakingViewStateFactory.createStashNoneState(
            assetSelectorMixin.selectedAssetFlow,
            accountStakingState,
            stakingStateScope,
            ::showError
        )

        is StakingState.NonStash -> stakingViewStateFactory.createWelcomeViewState(
            stakingStateScope,
            ::showError,
            assetSelectorMixin.selectedAssetFlow,
        )

        is StakingState.Stash.Validator -> stakingViewStateFactory.createValidatorViewState(
            accountStakingState,
            assetSelectorMixin.selectedAssetFlow,
            stakingStateScope,
            ::showError
        )
    }

    private fun transformNetworkInfo(asset: Asset, networkInfo: NetworkInfo): StakingNetworkInfoModel {
        val totalStaked = mapAmountToAmountModel(networkInfo.totalStake, asset)
        val minimumStake = mapAmountToAmountModel(networkInfo.minimumStake, asset)

        val unstakingPeriod = resourceManager.getQuantityString(R.plurals.staking_main_lockup_period_value, networkInfo.lockupPeriodInDays)
            .format(networkInfo.lockupPeriodInDays)

        val stakingPeriod = when (networkInfo.stakingPeriod) {
            StakingPeriod.Unlimited -> resourceManager.getString(R.string.common_unlimited)
        }

        return with(networkInfo) {
            StakingNetworkInfoModel(
                totalStaked = totalStaked,
                minimumStake = minimumStake,
                activeNominators = nominatorsCount.format(),
                stakingPeriod = stakingPeriod,
                unstakingPeriod = unstakingPeriod
            )
        }
    }

    private fun currentAddressModelFlow(): Flow<AddressModel> {
        return interactor.selectedAccountProjectionFlow().map {
            addressIconGenerator.createAddressModel(it.address, CURRENT_ICON_SIZE, it.name)
        }
    }
}
