package io.novafoundation.nova.feature_staking_impl.presentation.mythos.redeem

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.MythosRedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.validations.RedeemMythosStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.validations.RedeemMythosValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MythosRedeemViewModel(
    private val router: MythosStakingRouter,
    private val resourceManager: ResourceManager,
    private val validationSystem: RedeemMythosValidationSystem,
    private val validationFailureFormatter: MythosStakingValidationFailureFormatter,
    private val interactor: MythosRedeemInteractor,
    private val feeLoaderMixin: FeeLoaderMixin.Presentation,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
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

    private val redeemableAmountFlow = interactor.redeemAmountFlow()
        .shareInBackground()

    val redeemableAmountModelFlow = combine(redeemableAmountFlow, assetFlow, ::mapAmountToAmountModel)
        .withSafeLoading()
        .shareInBackground()

    val currentAccountModelFlow = selectedAccountUseCase.selectedAddressModelFlow { selectedAssetState.chain() }
        .shareInBackground()

    val walletFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: StateFlow<Boolean> = _showNextProgress

    init {
        feeLoaderMixin.loadFee(
            coroutineScope = this,
            feeConstructor = { interactor.estimateFee() },
            onRetryCancelled = {}
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

    private fun sendTransactionIfValid() = launchUnit {
        _showNextProgress.value = true

        val payload = RedeemMythosStakingValidationPayload(
            fee = feeLoaderMixin.awaitFee(),
            asset = assetFlow.first()
        )

        val redeemAmount = redeemableAmountFlow.first()

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = validationFailureFormatter::formatRedeem,
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction(redeemAmount)
        }
    }

    private fun sendTransaction(redeemAmount: Balance) = launch {
        interactor.redeem(redeemAmount)
            .onFailure(::showError)
            .onSuccess { redeemConsequences ->
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))

                router.finishRedeemFlow(redeemConsequences)
            }

        _showNextProgress.value = false
    }
}
