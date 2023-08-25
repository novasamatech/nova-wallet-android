package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StartMultiStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.currentSelectionFlow
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.handleStartMultiStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType.MultiStakingSelectionTypeProviderFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.MultiStakingSelectionFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.toStakingOptionIds
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.ConfirmMultiStakingPayload
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.setupAmount.model.StakingPropertiesModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.amountChooser.AmountChooserMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWithV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.create
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeToParcel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val DEBOUNCE_RATE_MILLIS = 500

class SetupAmountMultiStakingViewModel(
    private val multiStakingSelectionFormatter: MultiStakingSelectionFormatter,
    private val resourceManager: ResourceManager,
    private val router: StartMultiStakingRouter,
    private val interactor: StartMultiStakingInteractor,
    private val validationExecutor: ValidationExecutor,
    multiStakingSelectionTypeProviderFactory: MultiStakingSelectionTypeProviderFactory,
    assetUseCase: ArbitraryAssetUseCase,
    amountChooserMixinFactory: AmountChooserMixin.Factory,
    selectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    private val payload: SetupAmountMultiStakingPayload,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory
) : BaseViewModel(),
    Validatable by validationExecutor {

    private val multiStakingSelectionTypeProvider = multiStakingSelectionTypeProviderFactory.create(
        scope = viewModelScope,
        candidateOptionsIds = payload.availableStakingOptions.toStakingOptionIds()
    )

    private val multiStakingSelectionTypeFlow = multiStakingSelectionTypeProvider.multiStakingSelectionTypeFlow()
        .shareInBackground()

    private val currentSelectionFlow = selectionStoreProvider.currentSelectionFlow(viewModelScope)
        .shareInBackground()

    val currentAssetFlow = assetUseCase.assetFlow(
        chainId = payload.availableStakingOptions.chainId,
        assetId = payload.availableStakingOptions.assetId
    ).shareInBackground()

    val availableBalance = combine(
        currentAssetFlow,
        multiStakingSelectionTypeFlow
    ) { currentAsset, multiStakingSelectionType ->
        multiStakingSelectionType.availableBalance(currentAsset)
    }.shareInBackground()

    val amountChooserMixin = amountChooserMixinFactory.create(
        scope = viewModelScope,
        assetFlow = currentAssetFlow,
        availableBalanceFlow = availableBalance,
        balanceLabel = R.string.wallet_balance_available
    )

    private val feeLoaderMixin = feeLoaderMixinFactory.create(currentAssetFlow)

    private val loadingInProgressFlow = MutableStateFlow(false)

    private val amountEmptyFlow = amountChooserMixin.amountInput
        .map { it.isEmpty() }
        .distinctUntilChanged()

    val stakingPropertiesModel = combine(
        amountEmptyFlow,
        currentSelectionFlow
    ) { amountEmpty, currentSelection ->
        when {
            currentSelection == null && amountEmpty -> StakingPropertiesModel.Hidden
            currentSelection == null -> StakingPropertiesModel.Loading
            else -> {
                val content = StakingPropertiesModel.Content(
                    estimatedReward = currentSelection.selection.apy.orZero().format(),
                    selection = multiStakingSelectionFormatter.formatForSetupAmount(currentSelection)
                )

                StakingPropertiesModel.Loaded(content)
            }
        }
    }.shareInBackground()

    val title = currentAssetFlow.map {
        val tokenSymbol = it.token.configuration.symbol

        resourceManager.getString(R.string.staking_stake_format, tokenSymbol)
    }.shareInBackground()

    val continueButtonState = combine(
        loadingInProgressFlow,
        currentSelectionFlow,
        amountEmptyFlow
    ) { loadingInProgress, currentSelection, amountEmpty ->
        when {
            loadingInProgress -> DescriptiveButtonState.Loading
            amountEmpty -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_enter_amount))
            currentSelection == null -> DescriptiveButtonState.Disabled(resourceManager.getString(R.string.common_continue))
            else -> DescriptiveButtonState.Enabled(resourceManager.getString(R.string.common_continue))
        }
    }.shareInBackground()

    init {
        runSelectionUpdates()

        runFeeUpdates()
    }

    fun back() {
        router.back()
    }

    fun selectionClicked() {
        router.openSetupStakingType()
    }

    fun continueClicked() = launch {
        val currentSelection = currentSelectionFlow.first()?.selection ?: return@launch
        loadingInProgressFlow.value = true

        val validationSystem = multiStakingSelectionTypeFlow.first().validationSystem(currentSelection)
        val payload = StartMultiStakingValidationPayload(
            selection = currentSelection,
            asset = currentAssetFlow.first(),
            fee = feeLoaderMixin.awaitDecimalFee()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { handleStartMultiStakingValidationFailure(it, resourceManager) },
            progressConsumer = loadingInProgressFlow.progressConsumer(),
        ) {
            loadingInProgressFlow.value = false

            openConfirm(it)
        }
    }

    private fun openConfirm(validPayload: StartMultiStakingValidationPayload) {
        val confirmPayload = ConfirmMultiStakingPayload(mapFeeToParcel(validPayload.fee), payload.availableStakingOptions)

        router.openConfirm(confirmPayload)
    }

    private fun runFeeUpdates() {
        feeLoaderMixin.connectWithV2(
            inputSource = currentSelectionFlow
                .filterNotNull()
                .debounce(DEBOUNCE_RATE_MILLIS.milliseconds),
            scope = viewModelScope,
            feeConstructor = { selection -> interactor.calculateFee(selection.selection) },
        )
    }

    private fun runSelectionUpdates() {
        combine(
            multiStakingSelectionTypeFlow,
            amountChooserMixin.amountInput
        ) { multiStakingSelectionType, amountInput ->
            val amount = amountInput.toBigDecimalOrNull() ?: return@combine
            val asset = currentAssetFlow.first()
            val planks = asset.token.planksFromAmount(amount)

            multiStakingSelectionType.updateSelectionFor(planks)
        }
            .inBackground()
            .launchIn(viewModelScope)
    }
}
