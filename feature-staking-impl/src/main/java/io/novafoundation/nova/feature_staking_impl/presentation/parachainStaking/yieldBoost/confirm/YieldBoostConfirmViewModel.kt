package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.input.Input
import io.novafoundation.nova.common.utils.input.disabledInput
import io.novafoundation.nova.common.utils.input.modifiableInput
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper

import io.novafoundation.nova.feature_staking_api.domain.model.parachain.DelegatorState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostConfiguration
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.YieldBoostTask
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.yieldBoost.validations.YieldBoostValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.ParachainStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.collator.select.model.mapCollatorParcelModelToCollator
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.common.collators.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.common.formatDaysFrequency
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.common.yieldBoostValidationFailure
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfiguration
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfigurationModel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfigurationParcel
import io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.yieldBoost.confirm.model.YieldBoostConfirmPayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class YieldBoostConfirmViewModel(
    private val router: ParachainStakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val validationSystem: YieldBoostValidationSystem,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val interactor: YieldBoostInteractor,
    private val payload: YieldBoostConfirmPayload,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    selectedAccountUseCase: SelectedAccountUseCase,
    assetUseCase: AssetUseCase,
    walletUiUseCase: WalletUiUseCase,
    private val amountFormatter: AmountFormatter
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val decimalFee = mapFeeFromParcel(payload.fee)

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val collator by lazyAsync(Dispatchers.Default) {
        mapCollatorParcelModelToCollator(payload.collator)
    }

    private val chain by lazyAsync {
        selectedAssetState.chain()
    }

    private val yieldBoostConfiguration by lazyAsync {
        YieldBoostConfiguration(payload.configurationParcel)
    }

    private val _showNextProgress = MutableStateFlow(false)

    private val activeTasksFlow = delegatorStateUseCase.currentDelegatorStateFlow()
        .filterIsInstance<DelegatorState.Delegator>()
        .flatMapLatest(interactor::activeYieldBoostTasks)
        .shareInBackground()

    val currentAccountModelFlow = selectedAccountUseCase.selectedAddressModelFlow(chain::await)
        .shareInBackground()

    val walletFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val collatorAddressModel = flowOf {
        addressIconGenerator.collatorAddressModel(collator(), chain())
    }.shareInBackground()

    val termsCheckedFlow = MutableStateFlow(initialTermsCheckedInput())

    val buttonState = combine(termsCheckedFlow, _showNextProgress) { termsChecked, showNextProgress ->
        when {
            showNextProgress -> DescriptiveButtonState.Loading

            termsChecked is Input.Enabled.Modifiable && !termsChecked.value -> {
                DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_accept_terms))
            }

            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_confirm))
        }
    }.shareInBackground()

    val yieldBoostConfigurationUi = flowOf {
        mapConfigurationToUi(yieldBoostConfiguration())
    }.shareInBackground()

    init {
        setInitialFee()
    }

    fun confirmClicked() {
        sendTransactionIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        val address = currentAccountModelFlow.first().address

        externalActions.showAddressActions(address, selectedAssetState.chain())
    }

    private fun setInitialFee() = launch {
        feeLoaderMixin.setFee(decimalFee)
    }

    private fun sendTransactionIfValid() = launch {
        val payload = YieldBoostValidationPayload(
            collator = collator(),
            configuration = yieldBoostConfiguration(),
            fee = decimalFee,
            activeTasks = activeTasksFlow.first(),
            asset = assetFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { yieldBoostValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction(it.activeTasks, it.configuration)
        }
    }

    private fun sendTransaction(
        activeTasks: List<YieldBoostTask>,
        yieldBoostConfiguration: YieldBoostConfiguration,
    ) = launch {
        val outcome = withContext(Dispatchers.Default) {
            interactor.setYieldBoost(
                configuration = yieldBoostConfiguration,
                activeTasks = activeTasks
            )
        }

        outcome.onFailure(::showError)
            .onSuccess {
                showToast(resourceManager.getString(R.string.common_transaction_submitted))

                startNavigation(it.submissionHierarchy) { router.returnToStakingMain() }
            }

        _showNextProgress.value = false
    }

    private fun initialTermsCheckedInput(): Input<Boolean> = when (payload.configurationParcel) {
        is YieldBoostConfigurationParcel.Off -> disabledInput()
        is YieldBoostConfigurationParcel.On -> false.modifiableInput()
    }

    private suspend fun mapConfigurationToUi(configuration: YieldBoostConfiguration) = when (configuration) {
        is YieldBoostConfiguration.Off -> YieldBoostConfigurationModel(
            mode = resourceManager.getString(R.string.staking_turing_destination_payout),
            frequency = null,
            threshold = null,
            termsText = null
        )
        is YieldBoostConfiguration.On -> {
            val asset = assetFlow.first()

            val threshold = amountFormatter.formatAmountToAmountModel(configuration.threshold, asset)
            val frequency = resourceManager.formatDaysFrequency(configuration.frequencyInDays)

            val termsText = resourceManager.getString(R.string.yield_boost_terms, frequency, threshold.token)

            YieldBoostConfigurationModel(
                mode = resourceManager.getString(R.string.staking_turing_destination_restake),
                frequency = frequency,
                threshold = threshold,
                termsText = termsText
            )
        }
    }
}
