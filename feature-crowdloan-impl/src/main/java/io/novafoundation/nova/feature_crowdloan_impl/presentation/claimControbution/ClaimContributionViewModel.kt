package io.novafoundation.nova.feature_crowdloan_impl.presentation.claimControbution

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.TitleAndMessage
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
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_crowdloan_impl.R
import io.novafoundation.nova.feature_crowdloan_impl.domain.claimContributions.ClaimContributionsInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.claimContributions.validation.ClaimContributionValidationFailure
import io.novafoundation.nova.feature_crowdloan_impl.domain.claimContributions.validation.ClaimContributionValidationPayload
import io.novafoundation.nova.feature_crowdloan_impl.domain.claimContributions.validation.ClaimContributionValidationSystem
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleNotEnoughFeeError
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.formatAmountToAmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.runtime.state.AnySelectedAssetOptionSharedState
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ClaimContributionViewModel(
    private val router: CrowdloanRouter,
    private val resourceManager: ResourceManager,
    private val validationSystem: ClaimContributionValidationSystem,
    private val interactor: ClaimContributionsInteractor,
    private val feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val selectedAssetState: AnySelectedAssetOptionSharedState,
    private val validationExecutor: ValidationExecutor,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    selectedAccountUseCase: SelectedAccountUseCase,
    assetUseCase: AssetUseCase,
    walletUiUseCase: WalletUiUseCase,
    private val amountFormatter: AmountFormatter
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions,
    ExtrinsicNavigationWrapper by extrinsicNavigationWrapper {

    private val assetFlow = assetUseCase.currentAssetFlow()
        .shareInBackground()

    private val claimableContributionsFlow = interactor.claimableContributions()
        .shareInBackground()

    val redeemableAmountModelFlow = combine(claimableContributionsFlow, assetFlow) { claimableContributions, asset ->
        amountFormatter.formatAmountToAmountModel(claimableContributions.totalContributed, asset)
    }
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
        originFeeMixin.connectWith(claimableContributionsFlow) { _, claimableContributions ->
            interactor.estimateFee(claimableContributions.contributions)
        }
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

        val payload = ClaimContributionValidationPayload(
            fee = originFeeMixin.awaitFee(),
            asset = assetFlow.first()
        )

        val claimableContributions = claimableContributionsFlow.first()

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = ::formatRedeemFailure,
            progressConsumer = _showNextProgress.progressConsumer()
        ) {
            sendTransaction(claimableContributions.contributions)
        }
    }

    private fun sendTransaction(redeemableContributions: List<Contribution>) = launch {
        interactor.claim(redeemableContributions)
            .onFailure(::showError)
            .onSuccess { submissionResult ->
                showToast(resourceManager.getString(R.string.common_transaction_submitted))
                startNavigation(submissionResult.submissionHierarchy) { router.back() }
            }

        _showNextProgress.value = false
    }

    private fun formatRedeemFailure(failure: ClaimContributionValidationFailure): TitleAndMessage {
        return when (failure) {
            is ClaimContributionValidationFailure.NotEnoughBalanceToPayFees -> handleNotEnoughFeeError(failure, resourceManager)
        }
    }
}
