package io.novafoundation.nova.feature_multisig_operations.presentation.details.general

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.bold
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.formatting.spannable.format
import io.novafoundation.nova.common.utils.formatting.spannable.highlightedText
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.utils.withLoadingShared
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.common.view.PrimaryButton
import io.novafoundation.nova.common.view.bottomSheet.action.ActionBottomSheetLauncherFactory
import io.novafoundation.nova.common.view.bottomSheet.action.ButtonPreferences
import io.novafoundation.nova.common.view.bottomSheet.action.CheckBoxPreferences
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.multisig.MultisigPendingOperationsService
import io.novafoundation.nova.feature_account_api.data.multisig.model.MultisigAction
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.data.multisig.model.userAction
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountUIUseCase
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.allSignatories
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdKeyIn
import io.novafoundation.nova.feature_account_api.domain.model.requireMultisigAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_account_api.presenatation.navigation.ExtrinsicNavigationWrapper
import io.novafoundation.nova.feature_multisig_operations.R
import io.novafoundation.nova.feature_multisig_operations.domain.details.MultisigOperationDetailsInteractor
import io.novafoundation.nova.feature_multisig_operations.domain.details.validations.ApproveMultisigOperationValidationFailure
import io.novafoundation.nova.feature_multisig_operations.domain.details.validations.ApproveMultisigOperationValidationPayload
import io.novafoundation.nova.feature_multisig_operations.domain.details.validations.ApproveMultisigOperationValidationSystem
import io.novafoundation.nova.feature_multisig_operations.presentation.MultisigOperationsRouter
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallDetailsModel
import io.novafoundation.nova.feature_multisig_operations.presentation.callFormatting.MultisigCallFormatter
import io.novafoundation.nova.feature_multisig_operations.presentation.details.common.MultisigOperationDetailsPayload
import io.novafoundation.nova.feature_multisig_operations.presentation.details.general.adapter.SignatoryRvItem
import io.novafoundation.nova.feature_multisig_operations.presentation.enterCall.MultisigOperationEnterCallPayload
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatTokenAmount
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.connectWith
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefault
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.utilityAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class MultisigOperationDetailsViewModel(
    private val router: MultisigOperationsRouter,
    private val resourceManager: ResourceManager,
    private val interactor: MultisigOperationDetailsInteractor,
    private val multisigOperationsService: MultisigPendingOperationsService,
    private val feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    private val externalActions: ExternalActions.Presentation,
    private val validationExecutor: ValidationExecutor,
    private val payload: MultisigOperationDetailsPayload,
    private val validationSystem: ApproveMultisigOperationValidationSystem,
    private val extrinsicNavigationWrapper: ExtrinsicNavigationWrapper,
    private val signatoryListFormatter: SignatoryListFormatter,
    private val multisigCallFormatter: MultisigCallFormatter,
    private val actionBottomSheetLauncherFactory: ActionBottomSheetLauncherFactory,
    private val accountInteractor: AccountInteractor,
    private val accountUIUseCase: AccountUIUseCase,
    selectedAccountUseCase: SelectedAccountUseCase,
    walletUiUseCase: WalletUiUseCase,
) : BaseViewModel(),
    Validatable by validationExecutor,
    ExternalActions by externalActions {

    private val operationFlow = multisigOperationsService.pendingOperationFlow(payload.operationId)
        .filterNotNull()
        .shareInBackground()

    private val isLastOperationFlow = flowOf {
        val operationsCount = multisigOperationsService.getPendingOperationsCount()
        operationsCount == 1
    }
        .shareInBackground()

    private val chainFlow = operationFlow.map { it.chain }
        .shareInBackground()

    private val chainAssetFlow = chainFlow.map { it.utilityAsset }
        .shareInBackground()

    val chainUiFlow = chainFlow.map { mapChainToUi(it) }
        .shareInBackground()

    private val selectedAccountFlow = selectedAccountUseCase.selectedMetaAccountFlow()
        .map { it.requireMultisigAccount() }
        .shareInBackground()

    val walletFlow = walletUiUseCase.selectedWalletUiFlow(showAddressIcon = true)
        .shareInBackground()

    val formattedCall = combine(
        selectedAccountFlow,
        operationFlow
    ) { metaAccount, operation ->
        val initialOrigin = metaAccount.requireAccountIdKeyIn(operation.chain)
        multisigCallFormatter.formatDetails(operation.call, initialOrigin, operation.chain)
    }.shareInBackground()

    private val signatory = operationFlow
        .map { it.signatoryMetaId }
        .distinctUntilChanged()
        .flatMapLatest(interactor::signatoryFlow)
        .shareInBackground()

    val signatoryAccount = signatory.map { walletUiUseCase.walletUiFor(it) }
        .shareInBackground()

    val signatoriesTitle = combine(
        selectedAccountFlow,
        operationFlow
    ) { metaAccount, operation ->
        resourceManager.getString(R.string.multisig_operation_details_signatories, operation.approvals.size, metaAccount.threshold)
    }.shareInBackground()

    private val signatoryAccounts = selectedAccountFlow.map { it.allSignatories() }
        .distinctUntilChanged()
        .map { accountUIUseCase.getAccountModels(it, chainFlow.first()) }
        .shareInBackground()

    val formattedSignatories = combine(
        signatory,
        signatoryAccounts,
        operationFlow
    ) { currentSignatory, allSignatories, operation ->
        signatoryListFormatter.formatSignatories(
            chain = chainFlow.first(),
            currentSignatory = currentSignatory,
            signatories = allSignatories,
            approvals = operation.approvals.toSet()
        )
    }.withLoadingShared()
        .shareInBackground()

    private val showNextProgress = MutableStateFlow(false)

    val feeLoaderMixin = feeLoaderMixinV2Factory.createDefault(viewModelScope, chainAssetFlow)

    val showCallButtonState = operationFlow.map { it.call == null }
        .shareInBackground()

    val actionButtonState = combine(showNextProgress, operationFlow) { submissionInProgress, operation ->
        val action = operation.userAction()

        when {
            submissionInProgress -> DescriptiveButtonState.Loading

            action is MultisigAction.CanApprove -> when {

                operation.call == null -> DescriptiveButtonState.Gone

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
        when {
            operation.userAction() is MultisigAction.CanReject -> PrimaryButton.Appearance.PRIMARY_NEGATIVE
            else -> PrimaryButton.Appearance.PRIMARY
        }
    }.shareInBackground()

    val callDetailsVisible = operationFlow
        .map { operation -> operation.call != null }
        .shareInBackground()

    val actionBottomSheetLauncher = actionBottomSheetLauncherFactory.create()

    init {
        loadFee()
    }

    fun enterCallDataClicked() {
        router.openEnterCallDetails(MultisigOperationEnterCallPayload(payload.operationId))
    }

    fun actionClicked() {
        launch {
            val operation = operationFlow.first()

            val isReject = operation.userAction() == MultisigAction.CanReject
            if (isReject) {
                confirmReject(operation.getDepositorName())
            }

            sendTransactionIfValid()
        }
    }

    private suspend fun PendingMultisigOperation.getDepositorName(): String {
        val depositorAccount = withContext(Dispatchers.Default) { accountInteractor.findMetaAccount(chain, depositor.value) }
        return depositorAccount?.name ?: chain.addressOf(depositor)
    }

    private suspend fun confirmReject(depositorName: String) = suspendCancellableCoroutine<Unit> {
        if (interactor.getSkipRejectConfirmation()) {
            it.resume(Unit)
            return@suspendCancellableCoroutine
        }

        var isAutoContinueChecked = false

        actionBottomSheetLauncher.launchBottomSheet(
            imageRes = R.drawable.ic_multisig,
            title = resourceManager.getString(R.string.multisig_signing_warning_title),
            subtitle = resourceManager.highlightedText(R.string.multisig_signing_reject_confirmation_subtitle, depositorName),
            actionButtonPreferences = ButtonPreferences(
                text = resourceManager.getString(R.string.common_confirm),
                style = PrimaryButton.Appearance.PRIMARY,
                onClick = {
                    interactor.setSkipRejectConfirmation(isAutoContinueChecked)
                    it.resume(Unit)
                }
            ),
            neutralButtonPreferences = ButtonPreferences(
                text = resourceManager.getString(R.string.common_cancel),
                style = PrimaryButton.Appearance.SECONDARY,
                onClick = { it.cancel() }
            ),
            checkBoxPreferences = CheckBoxPreferences(
                text = resourceManager.getString(R.string.common_check_box_auto_continue),
                onCheckChanged = { isAutoContinueChecked = it }
            )
        )
    }

    fun backClicked() {
        router.back()
    }

    fun callDetailsClicked() = launch {
        router.openMultisigFullDetails(payload)
    }

    private fun loadFee() {
        feeLoaderMixin.connectWith(operationFlow) { _, operation ->
            interactor.estimateActionFee(operation)
        }
    }

    private fun sendTransactionIfValid() = launchUnit {
        showNextProgress.value = true

        val signatory = signatory.first()
        val operation = operationFlow.first()

        val signatoryBalance = interactor.getSignatoryBalance(signatory, operation.chain)
            .onFailure {
                showError(it)
                showNextProgress.value = false
            }
            .getOrNull() ?: return@launchUnit

        val payload = ApproveMultisigOperationValidationPayload(
            fee = feeLoaderMixin.awaitFee(),
            signatoryBalance = signatoryBalance,
            signatory = signatory,
            operation = operation,
            multisig = selectedAccountFlow.first()
        )

        validationExecutor.requireValid(
            validationSystem = validationSystem,
            payload = payload,
            validationFailureTransformer = ::formatValidationFailure,
            progressConsumer = showNextProgress.progressConsumer()
        ) {
            sendTransaction()
        }
    }

    private fun formatValidationFailure(failure: ApproveMultisigOperationValidationFailure): TitleAndMessage {
        return when (failure) {
            is ApproveMultisigOperationValidationFailure.NotEnoughBalanceToPayFees -> {
                val title = resourceManager.getString(R.string.common_error_not_enough_tokens)
                val message = SpannableFormatter.format(
                    resourceManager,
                    R.string.multisig_signatory_validation_ed,
                    failure.signatory.name.bold(),
                    failure.minimumNeeded.formatTokenAmount(failure.chainAsset),
                    failure.available.formatTokenAmount(failure.chainAsset),
                )

                title to message
            }

            ApproveMultisigOperationValidationFailure.TransactionIsNotAvailable -> {
                resourceManager.getString(R.string.multisig_approve_transaction_unavailable_title) to
                    resourceManager.getString(R.string.multisig_approve_transaction_unavailable_message)
            }
        }
    }

    private fun sendTransaction() = launch {
        interactor.performAction(operationFlow.first())
            .onFailure(::showError)
            .onSuccess {
                showToast(resourceManager.getString(R.string.common_transaction_submitted))

                extrinsicNavigationWrapper.startNavigation(it.submissionHierarchy) {
                    val isLeastOperation = isLastOperationFlow.first()

                    if (isLeastOperation) {
                        router.openMain()
                    } else {
                        router.back()
                    }
                }
            }

        showNextProgress.value = false
    }

    fun onSignatoryClicked(signatoryRvItem: SignatoryRvItem) = launchUnit {
        showAddressActionForOriginChain(signatoryRvItem.accountModel.address())
    }

    fun walletDetailsClicked() = launchUnit {
        val metaAccount = selectedAccountFlow.first()
        val chain = chainFlow.first()
        externalActions.showAddressActions(metaAccount, chain)
    }

    fun onTableAccountClicked(tableAccount: MultisigCallDetailsModel.TableValue.Account) = launchUnit {
        externalActions.showAddressActions(tableAccount.addressModel.address, tableAccount.chain)
    }

    fun behalfOfClicked() = launchUnit {
        val behalfOf = formattedCall.first().onBehalfOf ?: return@launchUnit
        showAddressActionForOriginChain(behalfOf.address)
    }

    fun signatoryDetailsClicked() = launchUnit {
        val metaAccount = signatory.first()
        val chain = chainFlow.first()
        externalActions.showAddressActions(metaAccount, chain)
    }

    private suspend fun showAddressActionForOriginChain(address: String) {
        externalActions.showAddressActions(address, chainFlow.first())
    }
}
