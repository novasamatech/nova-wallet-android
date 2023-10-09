package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.redeem

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.withLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.DelegatorStateUseCase
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.ParachainStakingRedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.validations.ParachainStakingRedeemValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.redeem.validations.ParachainStakingRedeemValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.math.BigDecimal

class ParachainStakingRedeemViewModel(
    private val router: StakingRouter,
    private val addressIconGenerator: AddressIconGenerator,
    private val resourceManager: ResourceManager,
    private val validationSystem: ParachainStakingRedeemValidationSystem,
    private val interactor: ParachainStakingRedeemInteractor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val delegatorStateUseCase: DelegatorStateUseCase,
    selectedAccountUseCase: SelectedAccountUseCase,
    assetUseCase: AssetUseCase,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel(),
    Retriable,
    Validatable by validationExecutor,
    FeeLoaderMixin by feeLoaderMixin,
    ExternalActions by externalActions {

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val delegatorState = delegatorStateUseCase.currentDelegatorStateFlow()
        .shareInBackground()

    val redeemableAmount = delegatorState.flatMapLatest { delegatorState ->
        val amount = interactor.redeemableAmount(delegatorState)

        assetFlow.map { asset ->
            mapAmountToAmountModel(amount, asset)
        }
    }
        .withLoading()
        .shareInBackground()

    val currentAccountModelFlow = selectedAccountUseCase.selectedMetaAccountFlow().map {
        addressIconGenerator.createAccountAddressModel(
            chain = selectedAssetState.chain(),
            account = it,
            name = null
        )
    }.shareInBackground()

    val walletFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: StateFlow<Boolean> = _showNextProgress

    init {
        feeLoaderMixin.connectWith(
            inputSource = delegatorState,
            scope = this,
            feeConstructor = { delegatorState -> interactor.estimateFee(delegatorState) },
            onRetryCancelled = ::backClicked
        )
    }

    fun confirmClicked() {
        sendTransactionIfValid()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        val address = currentAccountModelFlow.first().address

        externalActions.showExternalActions(ExternalActions.Type.Address(address), selectedAssetState.chain())
    }

    private fun sendTransactionIfValid() = requireFee { fee ->
        launch {
            val payload = ParachainStakingRedeemValidationPayload(
                fee = fee,
                asset = assetFlow.first()
            )

            validationExecutor.requireValid(
                validationSystem = validationSystem,
                payload = payload,
                validationFailureTransformer = { parachainStakingRedeemValidationFailure(it, resourceManager) },
                progressConsumer = _showNextProgress.progressConsumer()
            ) {
                sendTransaction()
            }
        }
    }

    private fun sendTransaction() = launch {
        interactor.redeem(delegatorState.first())
            .onFailure(::showError)
            .onSuccess { redeemConsequences ->
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                router.finishRedeemFlow(redeemConsequences)
            }

        _showNextProgress.value = false
    }

    private fun requireFee(block: (BigDecimal) -> Unit) = feeLoaderMixin.requireFee(
        block,
        onError = { title, message -> showError(title, message) }
    )
}
