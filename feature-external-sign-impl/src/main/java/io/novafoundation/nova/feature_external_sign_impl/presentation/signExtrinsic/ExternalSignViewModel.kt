package io.novafoundation.nova.feature_external_sign_impl.presentation.signExtrinsic

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.TransformedFailure
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.ValidationFlowActions
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator.Response
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignResponder
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignWallet
import io.novafoundation.nova.feature_external_sign_impl.ExternalSignRouter
import io.novafoundation.nova.feature_external_sign_impl.R
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ConfirmDAppOperationValidationFailure
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ConfirmDAppOperationValidationPayload
import io.novafoundation.nova.feature_external_sign_impl.domain.sign.ExternalSignInteractor
import io.novafoundation.nova.feature_wallet_api.domain.validation.handleFeeSpikeDetected
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model.PaymentCurrencySelectionMode
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.FeeLoaderMixinV2
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.asFeeContextFromSelf
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.awaitOptionalFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.v2.createDefaultBy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private typealias SigningError = String

class ExternalSignViewModel(
    private val router: ExternalSignRouter,
    private val responder: ExternalSignResponder,
    private val interactor: ExternalSignInteractor,
    private val payload: ExternalSignPayload,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    walletUiUseCase: WalletUiUseCase,
    feeLoaderMixinV2Factory: FeeLoaderMixinV2.Factory,
    actionAwaitableMixinFactory: ActionAwaitableMixin.Factory
) : BaseViewModel(),
    Validatable by validationExecutor {

    val confirmUnrecoverableError = actionAwaitableMixinFactory.confirmingAction<SigningError>()

    private val commissionTokenFlow = interactor.utilityAssetFlow()
        ?.shareInBackground()

    val originFeeMixin = commissionTokenFlow?.let {
        feeLoaderMixinV2Factory.createDefaultBy(
            scope = viewModelScope,
            feeContext = it.asFeeContextFromSelf(),
            configuration = FeeLoaderMixinV2.Configuration(
                showZeroFiat = false,
                initialState = FeeLoaderMixinV2.Configuration.InitialState(
                    paymentCurrencySelectionMode = PaymentCurrencySelectionMode.DETECT_FROM_FEE
                )
            )
        )
    }

    private val _performingOperationInProgress = MutableStateFlow(false)
    val performingOperationInProgress: StateFlow<Boolean> = _performingOperationInProgress

    val walletUi = walletUiUseCase.walletUiFor(payload.wallet)
        .shareInBackground()

    val requestedAccountModel = flowOf {
        interactor.createAccountAddressModel()
    }
        .shareInBackground()

    val maybeChainUi = flowOf {
        interactor.chainUi()
    }
        .finishOnFailure()
        .shareInBackground()

    val dAppInfo = payload.dappMetadata

    init {
        maybeLoadFee()
    }

    fun rejectClicked() {
        responder.respond(Response.Rejected(payload.signRequest.id))

        exit()
    }

    fun acceptClicked() = launch {
        _performingOperationInProgress.value = true

        val validationPayload = ConfirmDAppOperationValidationPayload(
            fee = originFeeMixin?.awaitOptionalFee()
        )

        validationExecutor.requireValid(
            validationSystem = interactor.validationSystem,
            payload = validationPayload,
            validationFailureTransformerCustom = ::validationFailureToUi,
            autoFixPayload = ::autoFixPayload,
            progressConsumer = _performingOperationInProgress.progressConsumer()
        ) {
            performOperation(it.fee)
        }
    }

    private fun performOperation(upToDateFee: Fee?) = launch {
        interactor.performOperation(upToDateFee)?.let { response ->
            responder.respond(response)

            exit()
        }

        _performingOperationInProgress.value = false
    }

    private fun maybeLoadFee() {
        originFeeMixin?.loadFee { interactor.calculateFee() }
    }

    private suspend fun respondError(errorMessage: String?) = withContext(Dispatchers.Main) {
        val shouldPresent = if (errorMessage != null) {
            confirmUnrecoverableError.awaitAction(errorMessage)
            false
        } else {
            true
        }

        val response = Response.SigningFailed(payload.signRequest.id, shouldPresent)

        responder.respond(response)
        exit()
    }

    private suspend fun respondError(error: Throwable) {
        val errorMessage = when (error) {
            is ExternalSignInteractor.Error.UnsupportedChain -> resourceManager.getString(R.string.dapp_sign_error_unsupported_chain, error.chainId)
            else -> null
        }

        respondError(errorMessage)
    }

    fun detailsClicked() {
        launch {
            val extrinsicContent = interactor.readableOperationContent()

            router.openExtrinsicDetails(extrinsicContent)
        }
    }

    private fun exit() = launch {
        interactor.shutdown()

        router.back()
    }

    private fun validationFailureToUi(
        failure: ValidationStatus.NotValid<ConfirmDAppOperationValidationFailure>,
        actions: ValidationFlowActions<*>
    ): TransformedFailure? {
        return when (val reason = failure.reason) {
            is ConfirmDAppOperationValidationFailure.FeeSpikeDetected -> originFeeMixin?.let {
                handleFeeSpikeDetected(
                    error = reason,
                    resourceManager = resourceManager,
                    setFee = originFeeMixin,
                    actions = actions
                )
            }
        }
    }

    private fun autoFixPayload(
        payload: ConfirmDAppOperationValidationPayload,
        failure: ConfirmDAppOperationValidationFailure
    ): ConfirmDAppOperationValidationPayload {
        return when (failure) {
            is ConfirmDAppOperationValidationFailure.FeeSpikeDetected -> payload.copy(fee = failure.payload.newFee)
        }
    }

    private fun <T> Flow<Result<T>>.finishOnFailure(): Flow<T?> {
        return onEach { result -> result.onFailure { respondError(it) } }
            .map { it.getOrNull() }
    }

    private fun WalletUiUseCase.walletUiFor(externalSignWallet: ExternalSignWallet): Flow<WalletModel> {
        return when (externalSignWallet) {
            ExternalSignWallet.Current -> selectedWalletUiFlow(showAddressIcon = true)
            is ExternalSignWallet.WithId -> walletUiFlow(externalSignWallet.metaId, showAddressIcon = true)
        }
    }
}
