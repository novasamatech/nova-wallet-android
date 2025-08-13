package io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper

import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.UnbondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.unbond.UnbondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.hints.UnbondHintsMixinFactory
import io.novafoundation.nova.feature_staking_impl.presentation.staking.unbond.unbondValidationFailure
import io.novafoundation.nova.feature_wallet_api.data.mappers.mapFeeToFeeModel
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.FeeStatus
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.mapFeeFromParcel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmUnbondViewModel(
    private val router: StakingRouter,
    interactor: StakingInteractor,
    private val unbondInteractor: UnbondInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val iconGenerator: AddressIconGenerator,
    private val validationSystem: UnbondValidationSystem,
    private val externalActions: ExternalActions.Presentation,
    private val payload: ConfirmUnbondPayload,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    unbondHintsMixinFactory: UnbondHintsMixinFactory,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel(),
    ExternalActions by externalActions,
    Validatable by validationExecutor,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val decimalFee = mapFeeFromParcel(payload.fee)

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    val hintsMixin = unbondHintsMixinFactory.create(coroutineScope = this)

    private val accountStakingFlow = interactor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val assetFlow = accountStakingFlow.flatMapLatest {
        interactor.assetFlow(it.controllerAddress)
    }
        .inBackground()
        .share()

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val amountModelFlow = assetFlow.map { asset ->
        mapAmountToAmountModel(payload.amount, asset)
    }
        .shareInBackground()

    val feeStatusLiveData = assetFlow.map { asset ->
        val feeModel = mapFeeToFeeModel(decimalFee, asset.token)

        FeeStatus.Loaded(feeModel)
    }
        .inBackground()
        .asLiveData()

    val originAddressModelFlow = accountStakingFlow.map {
        iconGenerator.createAccountAddressModel(it.chain, it.controllerAddress)
    }
        .shareInBackground()

    fun confirmClicked() {
        maybeGoToNext()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() {
        launch {
            val address = originAddressModelFlow.first().address

            externalActions.showAddressActions(address, selectedAssetState.chain())
        }
    }

    private fun maybeGoToNext() = launch {
        val asset = assetFlow.first()

        val payload = UnbondValidationPayload(
            asset = asset,
            stash = accountStakingFlow.first(),
            fee = decimalFee,
            amount = payload.amount,
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformerCustom = { status, flowActions -> unbondValidationFailure(status, flowActions, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer()
        ) { validPayload ->
            sendTransaction(validPayload)
        }
    }

    private fun sendTransaction(validPayload: UnbondValidationPayload) = launch {
        val amountInPlanks = validPayload.asset.token.configuration.planksFromAmount(payload.amount)

        unbondInteractor.unbond(validPayload.stash, validPayload.asset.bondedInPlanks, amountInPlanks)
            .onSuccess {
                showToast(resourceManager.getString(R.string.common_transaction_submitted))

                startNavigation(it.submissionHierarchy) { router.returnToStakingMain() }
            }.onFailure {
                showError(it)
            }

        _showNextProgress.value = false
    }
}
