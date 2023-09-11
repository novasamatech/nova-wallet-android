package io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.components
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StakingStartedDetectionService
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.StartMultiStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.activateDetection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.pauseDetection
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.copyWith
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.StartMultiStakingSelectionStoreProvider
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.selection.store.currentSelectionFlow
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.StartMultiStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.common.validations.handleStartMultiStakingValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.staking.start.setupAmount.selectionType.MultiStakingSelectionTypeProviderFactory
import io.novafoundation.nova.feature_staking_impl.presentation.StartMultiStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.common.toStakingOptionIds
import io.novafoundation.nova.feature_staking_impl.presentation.staking.start.confirm.types.ConfirmMultiStakingTypeFactory
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.ArbitraryAssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ConfirmMultiStakingViewModel(
    private val router: StartMultiStakingRouter,
    private val interactor: StartMultiStakingInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val externalActions: ExternalActions.Presentation,
    private val confirmMultiStakingTypeFactory: ConfirmMultiStakingTypeFactory,
    private val selectionStoreProvider: StartMultiStakingSelectionStoreProvider,
    payload: ConfirmMultiStakingPayload,
    selectionTypeProviderFactory: MultiStakingSelectionTypeProviderFactory,
    assetUseCase: ArbitraryAssetUseCase,
    walletUiUseCase: WalletUiUseCase,
    selectedAccountUseCase: SelectedAccountUseCase,
    private val stakingStartedDetectionService: StakingStartedDetectionService,
    ) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor {

    private val decimalFee = mapFeeFromParcel(payload.fee)

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: Flow<Boolean> = _showNextProgress

    private val currentSelectionFlow = selectionStoreProvider.currentSelectionFlow(viewModelScope)
        .filterNotNull()
        .shareInBackground()

    private val stakingTypeContext = ConfirmMultiStakingTypeFactory.Context(
        externalActions = externalActions,
        scope = viewModelScope
    )

    private val confirmMultiStakingTypeFlow = currentSelectionFlow.map { currentSelection ->
        confirmMultiStakingTypeFactory.constructConfirmMultiStakingType(
            selection = currentSelection.selection,
            parentContext = stakingTypeContext
        )
    }.shareInBackground()

    private val multiStakingSelectionTypeFlow = selectionTypeProviderFactory.create(viewModelScope, payload.availableStakingOptions.toStakingOptionIds())
        .multiStakingSelectionTypeFlow()
        .shareInBackground()

    val stakingTypeModel = confirmMultiStakingTypeFlow.flatMapLatest { it.stakingTypeModel }
        .shareInBackground()

    private val assetFlow = flowOfAll {
        val (chain, chainAsset) = currentSelectionFlow.first().selection.stakingOption.components

        assetUseCase.assetFlow(chain.id, chainAsset.id)
    }
        .shareInBackground()

    val amountModelFlow = combine(currentSelectionFlow, assetFlow) { currentSelection, asset ->
        mapAmountToAmountModel(currentSelection.selection.stake, asset)
    }
        .shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val feeStatusFlow = assetFlow.map { asset ->
        val feeModel = mapFeeToFeeModel(decimalFee.fee, asset.token)

        FeeStatus.Loaded(feeModel)
    }
        .shareInBackground()

    val originAddressModelFlow = selectedAccountUseCase.selectedAddressModelFlow { currentSelectionFlow.first().selection.stakingOption.chain }
        .shareInBackground()

    fun confirmClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        val address = originAddressModelFlow.first().address
        val chain = currentSelectionFlow.first().selection.stakingOption.chain

        externalActions.showAddressActions(address, chain)
    }

    fun onStakingTypeDetailsClicked() = launch {
        confirmMultiStakingTypeFlow.first().onStakingTypeDetailsClicked()
    }

    private fun maybeGoToNext() = launch {
        val recommendableSelection = currentSelectionFlow.first()
        val validationSystem = multiStakingSelectionTypeFlow.first().validationSystem(recommendableSelection.selection)

        val payload = StartMultiStakingValidationPayload(
            recommendableSelection = recommendableSelection,
            asset = assetFlow.first(),
            fee = decimalFee
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { status, flowActions ->
                handleStartMultiStakingValidationFailure(status, resourceManager, flowActions, ::updateAmount)
            },
            progressConsumer = _showNextProgress.progressConsumer(),
            block = ::sendTransaction
        )
    }

    private fun sendTransaction(validationPayload: StartMultiStakingValidationPayload) = launch {
        stakingStartedDetectionService.pauseDetection(viewModelScope)

        interactor.startStaking(validationPayload.selection)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                finishFlow()
            }
            .onFailure {
                showError(it)

                stakingStartedDetectionService.activateDetection(viewModelScope)
            }

        _showNextProgress.value = false
    }

    private fun finishFlow() {
        router.returnToStakingDashboard()
    }

    private fun updateAmount(newAmount: BigDecimal) = launch {
        val currentSelection = currentSelectionFlow.first()
        val newSelection = currentSelection.copyWith(newAmount)

        selectionStoreProvider.getSelectionStore(viewModelScope).updateSelection(newSelection)
    }
}
