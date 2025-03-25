package io.novafoundation.nova.feature_staking_impl.presentation.mythos.unbond.confirm

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.MythosSharedComputation
import io.novafoundation.nova.feature_staking_impl.domain.mythos.common.delegatorState
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.UnbondMythosStakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations.UnbondMythosStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.mythos.unbond.validations.UnbondMythosValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.collatorAddressModel
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.details.mythos
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.start.selectCollator.model.toDomain
import io.novafoundation.nova.feature_staking_impl.presentation.validators.details.StakeTargetDetailsPayload
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.toDomain
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmUnbondMythosViewModel(
    private val router: MythosStakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val validationSystem: UnbondMythosValidationSystem,
    private val interactor: UnbondMythosStakingInteractor,
    private val feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val mythosSharedComputation: MythosSharedComputation,
    private val payload: ConfirmUnbondMythosPayload,
    private val validationFailureFormatter: MythosStakingValidationFailureFormatter,
    selectedAccountUseCase: SelectedAccountUseCase,
    assetUseCase: AssetUseCase,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val fee = payload.fee.toDomain()

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val collator = payload.collator.toDomain()

    val currentAccountModelFlow = selectedAccountUseCase.selectedAddressModelFlow { selectedAssetState.chain() }
        .shareInBackground()

    val amountModel = assetFlow.map { asset ->
        mapAmountToAmountModel(payload.amount, asset)
    }
        .shareInBackground()

    val walletFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val collatorAddressModel = flowOf {
        addressIconGenerator.collatorAddressModel(collator, selectedAssetState.chain())
    }.shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: StateFlow<Boolean> = _showNextProgress

    val feeLoaderMixin = feeLoaderMixinV2Factory.createDefault(viewModelScope, selectedAssetState.selectedAssetFlow())

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

    fun collatorClicked() {
        router.openCollatorDetails(StakeTargetDetailsPayload.mythos(collator))
    }

    private fun setInitialFee() = launch {
        feeLoaderMixin.setFee(fee)
    }

    private fun sendTransactionIfValid() = launch {
        val payload = UnbondMythosStakingValidationPayload(
            fee = fee,
            collator = collator,
            asset = assetFlow.first(),
            delegatorState = mythosSharedComputation.delegatorState()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { failure, _ -> validationFailureFormatter.formatUnbond(failure) },
            progressConsumer = _showNextProgress.progressConsumer(),
            block = ::sendTransaction
        )
    }

    private fun sendTransaction(validPayload: UnbondMythosStakingValidationPayload) = launch {
        interactor.unbond(validPayload.delegatorState, validPayload.collator.accountId)
            .onFailure(::showError)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                router.returnToStakingMain()
            }

        _showNextProgress.value = false
    }
}
