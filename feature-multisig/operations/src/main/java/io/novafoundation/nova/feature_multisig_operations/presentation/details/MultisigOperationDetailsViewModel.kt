package io.novafoundation.nova.feature_multisig_operations.presentation.details

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.view.PrimaryButton
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.data.multisig.model.MultisigAction
import io.novafoundation.nova.feature_account_api.data.multisig.model.userAction
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.domain.details.MultisigOperationDetailsInteractor
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.common.MultisigOperationFormatter
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.runtime.ext.utilityAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// TODO multisig:
// 1. Validations
class MultisigOperationDetailsViewModel(
    private val router: MultisigOperationsRouter,
    private val resourceManager: ResourceManager,
    private val operationFormatter: MultisigOperationFormatter,
    private val interactor: MultisigOperationDetailsInteractor,
    private val multisigOperationsService: MultisigPendingOperationsService,
    private val feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val validationExecutor: ValidationExecutor,
    private val payload: MultisigOperationDetailsPayload,
    selectedAccountUseCase: SelectedAccountUseCase,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val operationFlow = multisigOperationsService.pendingOperationFlow(payload.operationId)
        .filterNotNull()
        .shareInBackground()

    val title = operationFlow.map { operationFormatter.formatTitle(it) }
        .shareInBackground()

    private val chainFlow = operationFlow.map { it.chain }
    private val chainAssetFlow = chainFlow.map { it.utilityAsset }

    val currentAccountModelFlow = selectedAccountUseCase.selectedAddressModelFlow { chainFlow.first() }
        .shareInBackground()

    val walletFlow = walletUiUseCase.selectedWalletUiFlow()
        .shareInBackground()

    private val showNextProgress = MutableStateFlow(false)

    val feeLoaderMixin = feeLoaderMixinV2Factory.createDefault(viewModelScope, chainAssetFlow)

    val buttonState = combine(showNextProgress, operationFlow) { submissionInProgress, operation ->
        val action = operation.userAction()

        when {
            submissionInProgress -> DescriptiveButtonState.Loading

            action is MultisigAction.CanApprove -> when {
                operation.call == null -> DescriptiveButtonState.Disabled(
                    reason = resourceManager.getString(R.string.multisig_operation_details_call_data_not_found)
                )

                action.isFinalApproval -> DescriptiveButtonState.Enabled(
                    action = resourceManager.getString(R.string.multisig_operation_details_approve_and_execute)
                )

                else -> DescriptiveButtonState.Enabled(
                    action = resourceManager.getString(R.string.multisig_operation_details_approve)
                )
            }

            action is MultisigAction.CanReject -> DescriptiveButtonState.Enabled(
                action = resourceManager.getString(R.string.multisig_operation_details_reject)
            )

            else -> DescriptiveButtonState.Gone
        }
    }.shareInBackground()

    val buttonAppearance = operationFlow.map { operation ->
        if (operation.userAction() == MultisigAction.CanReject) {
            PrimaryButton.Appearance.PRIMARY_NEGATIVE
        } else {
            PrimaryButton.Appearance.PRIMARY
        }
    }.shareInBackground()

    val callDetailsVisible = operationFlow
        .map { operation -> operation.call != null }
        .shareInBackground()

    init {
        loadFee()
    }

    fun actionClicked() {
        sendTransaction()
    }

    fun backClicked() {
        router.back()
    }

    fun originAccountClicked() = launch {
        val address = currentAccountModelFlow.first().address
        externalActions.showAddressActions(address, chainFlow.first())
    }

    fun callDetailsClicked() = launch {
        val operationCall = operationFlow.first().call ?: return@launch
        val readableContent = interactor.callDetails(operationCall)
        router.openMultisigCallDetails(readableContent)
    }

    private fun loadFee() {
        feeLoaderMixin.connectWith(operationFlow) { _, operation ->
            interactor.estimateActionFee(operation)
        }
    }

    private fun sendTransaction() = launch {
        showNextProgress.value = true

        interactor.performAction(operationFlow.first())
            .onFailure(::showError)
            .onSuccess {
                showMessage(resourceManager.getString(R.string.common_transaction_submitted))
                router.back()
            }

        showNextProgress.value = false
    }
}
