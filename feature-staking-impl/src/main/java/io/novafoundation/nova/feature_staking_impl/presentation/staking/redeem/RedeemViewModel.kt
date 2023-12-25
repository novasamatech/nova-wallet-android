package io.novafoundation.nova.feature_staking_impl.presentation.staking.redeem

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.redeem.RedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.reedeem.RedeemValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RedeemViewModel(
    private val router: StakingRouter,
    private val interactor: StakingInteractor,
    private val redeemInteractor: RedeemInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: RedeemValidationSystem,
    private val iconGenerator: AddressIconGenerator,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel(),
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val accountStakingFlow = interactor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .shareInBackground()

    private val assetFlow = accountStakingFlow
        .flatMapLatest { interactor.assetFlow(it.controllerAddress) }
        .shareInBackground()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val amountModelFlow = assetFlow.map { asset ->
        mapAmountToAmountModel(asset.redeemable, asset)
    }
        .shareInBackground()

    val originAddressModelFlow = accountStakingFlow.map {
        iconGenerator.createAccountAddressModel(selectedAssetState.chain(), it.controllerAddress)
    }
        .shareInBackground()

    init {
        loadFee()
    }

    fun confirmClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() {
        launch {
            val payload = ExternalActions.Type.Address(originAddressModelFlow.first().address)
            externalActions.showExternalActions(payload, selectedAssetState.chain())
        }
    }

    private fun loadFee() {
        feeLoaderMixin.loadFee(
            coroutineScope = viewModelScope,
            feeConstructor = { redeemInteractor.estimateFee(accountStakingFlow.first()) },
            onRetryCancelled = ::backClicked
        )
    }

    private fun maybeGoToNext() = launch {
        _showNextProgress.value = true

        val asset = assetFlow.first()

        val validationPayload = RedeemValidationPayload(
            fee = feeLoaderMixin.awaitDecimalFee(),
            asset = asset
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = validationPayload,
            validationFailureTransformer = { redeemValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction(it)
        }
    }

    private fun sendTransaction(redeemValidationPayload: RedeemValidationPayload) = launch {
        redeemInteractor.redeem(accountStakingFlow.first(), redeemValidationPayload.asset)
            .onSuccess { redeemConsequences ->
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                router.finishRedeemFlow(redeemConsequences)
            }
            .onFailure(::showError)

        _showNextProgress.value = false
    }
}
