package io.novafoundation.nova.feature_staking_impl.presentation.mythos.redeem

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper

import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.MythosRedeemInteractor
import io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.validations.RedeemMythosStakingValidationPayload
import io.novafoundation.nova.feature_staking_impl.domain.mythos.redeem.validations.RedeemMythosValidationSystem
import io.novafoundation.nova.feature_staking_impl.presentation.MythosStakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.mythos.common.validations.MythosStakingValidationFailureFormatter
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedAssetFlow
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
    private val feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    selectedAccountUseCase: SelectedAccountUseCase,
    assetUseCase: AssetUseCase,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

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

    val originFeeMixin = feeLoaderMixinV2Factory.createDefault(viewModelScope, selectedAssetState.selectedAssetFlow())

    private val _showNextProgress = MutableStateFlow(false)
    val showNextProgress: StateFlow<Boolean> = _showNextProgress

    init {
        originFeeMixin.loadFee { interactor.estimateFee() }
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

    private fun sendTransactionIfValid() = launchUnit {
        _showNextProgress.value = true

        val payload = RedeemMythosStakingValidationPayload(
            fee = originFeeMixin.awaitFee(),
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
            .onSuccess { (submissionResult, redeemConsequences) ->
                showToast(resourceManager.getString(R.string.common_transaction_submitted))

                startNavigation(submissionResult.submissionHierarchy) { router.finishRedeemFlow(redeemConsequences) }
            }

        _showNextProgress.value = false
    }
}
