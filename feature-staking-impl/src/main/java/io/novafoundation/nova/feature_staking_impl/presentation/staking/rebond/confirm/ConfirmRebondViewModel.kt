package io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.confirm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.mixin.hints.ResourcesHintsMixinFactory
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_staking_api.domain.model.relaychain.StakingState
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.StakingInteractor
import io.novafoundation.nova.feature_staking_impl.domain.staking.rebond.RebondInteractor
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.validations.rebond.RebondValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.rebond.rebondValidationFailure
import io.novafoundation.nova.feature_wallet_api.domain.model.planksFromAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ConfirmRebondViewModel(
    private val router: StakingRouter,
    interactor: StakingInteractor,
    private val rebondInteractor: RebondInteractor,
    private val resourceManager: ResourceManager,
    private val validationExecutor: ValidationExecutor,
    private val validationSystem: RebondValidationSystem,
    private val iconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val payload: ConfirmRebondPayload,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    hintsMixinFactory: ResourcesHintsMixinFactory,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel(),
    ExternalActions by externalActions,
    FeeLoaderMixin by feeLoaderMixin,
    Validatable by validationExecutor {

    private val _showNextProgress = MutableLiveData(false)
    val showNextProgress: LiveData<Boolean> = _showNextProgress

    private val accountStakingFlow = interactor.selectedAccountStakingStateFlow(viewModelScope)
        .filterIsInstance<StakingState.Stash>()
        .inBackground()
        .share()

    private val assetFlow = accountStakingFlow.flatMapLatest {
        interactor.assetFlow(it.controllerAddress)
    }
        .inBackground()
        .share()

    val hintsMixin = hintsMixinFactory.create(
        coroutineScope = this,
        hintsRes = listOf(R.string.staking_rebond_counted_next_era_hint)
    )

    val walletUiFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    val amountModelFlow = assetFlow.map { asset ->
        mapAmountToAmountModel(payload.amount, asset)
    }
        .shareInBackground()

    val originAddressModelFlow = accountStakingFlow.map {
        iconGenerator.createAccountAddressModel(it.chain, it.controllerAddress)
    }.shareInBackground()

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
            val address = originAddressModelFlow.first().address

            externalActions.showAddressActions(address, selectedAssetState.chain())
        }
    }

    private fun loadFee() {
        feeLoaderMixin.loadFee(
            coroutineScope = viewModelScope,
            feeConstructor = { token ->
                val amountInPlanks = token.planksFromAmount(payload.amount)

                rebondInteractor.estimateFee(amountInPlanks, accountStakingFlow.first())
            },
            onRetryCancelled = ::backClicked
        )
    }

    private fun maybeGoToNext() = launch {
        _showNextProgress.value = true

        val payload = RebondValidationPayload(
            fee = feeLoaderMixin.awaitFee(),
            rebondAmount = payload.amount,
            controllerAsset = assetFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = { rebondValidationFailure(it, resourceManager) },
            progressConsumer = _showNextProgress.progressConsumer(),
            block = ::sendTransaction
        )
    }

    private fun sendTransaction(validPayload: RebondValidationPayload) = launch {
        val amountInPlanks = validPayload.controllerAsset.token.planksFromAmount(payload.amount)
        val stashState = accountStakingFlow.first()

        rebondInteractor.rebond(stashState, amountInPlanks)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                router.returnToStakingMain()
            }
            .onFailure(::showError)

        _showNextProgress.value = false
    }
}
