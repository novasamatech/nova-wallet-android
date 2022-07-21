package io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.api.Validatable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.validation.ValidationExecutor
import io.novafoundation.nova.common.validation.progressConsumer
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.ConfirmDAppOperationValidationFailure
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.ConfirmDAppOperationValidationPayload
import io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic.DAppSignInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator.Response
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.FeeLoaderMixin
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.WithFeeLoaderMixin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DAppSignViewModel(
    private val router: DAppRouter,
    private val responder: DAppSignResponder,
    private val interactor: DAppSignInteractor,
    private val commonInteractor: DappInteractor,
    private val payload: DAppSignPayload,
    private val validationExecutor: ValidationExecutor,
    private val resourceManager: ResourceManager,
    walletUiUseCase: WalletUiUseCase,
    selectedAccountUseCase: SelectedAccountUseCase,
    feeLoaderMixinFactory: FeeLoaderMixin.Factory,
) : BaseViewModel(),
    WithFeeLoaderMixin,
    Validatable by validationExecutor {

    private val commissionTokenFlow = interactor.commissionTokenFlow()
        ?.shareInBackground()

    override val originFeeMixin: FeeLoaderMixin.Presentation? = commissionTokenFlow?.let {
        feeLoaderMixinFactory.create(
            tokenFlow = it,
            configuration = FeeLoaderMixin.Configuration(showZeroFiat = false)
        )
    }

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .share()

    private val _performingOperationInProgress = MutableStateFlow(false)
    val performingOperationInProgress: StateFlow<Boolean> = _performingOperationInProgress

    val walletUi = walletUiUseCase.selectedWalletUiFlow(showAddressIcon = true)
        .shareInBackground()

    val requestedAccountModel = selectedAccount.map {
        interactor.createAccountAddressModel()
    }
        .shareInBackground()

    val maybeChainUi = flowOf {
        interactor.chainUi()
    }
        .shareInBackground()

    val dAppInfo = flowOf { commonInteractor.getDAppInfo(payload.dappUrl) }
        .shareInBackground()

    init {
        maybeLoadFee()
    }

    fun cancelled() = rejectClicked()

    fun rejectClicked() {
        responder.respond(Response.Rejected(payload.body.id))

        exit()
    }

    fun acceptClicked() = launch {
        val validationPayload = ConfirmDAppOperationValidationPayload(
            token = commissionTokenFlow?.first()
        )

        validationExecutor.requireValid(
            validationSystem = interactor.validationSystem,
            payload = validationPayload,
            validationFailureTransformer = ::validationFailureToUi,
            progressConsumer = _performingOperationInProgress.progressConsumer()
        ) {
            performOperation()
        }
    }

    private fun performOperation() = launch {
        interactor.performOperation()?.let { response ->
            responder.respond(response)

            exit()
        }

        _performingOperationInProgress.value = false
    }

    private fun maybeLoadFee() {
        originFeeMixin?.loadFee(
            coroutineScope = this,
            feeConstructor = { interactor.calculateFee() },
            onRetryCancelled = {}
        )
    }

    fun detailsClicked() {
        launch {
            val extrinsicContent = interactor.readableOperationContent()

            router.openExtrinsicDetails(extrinsicContent)
        }
    }

    fun exit() {
        interactor.shutdown()
        router.back()
    }

    private fun validationFailureToUi(failure: ConfirmDAppOperationValidationFailure): TitleAndMessage {
        return when (failure) {
            ConfirmDAppOperationValidationFailure.NotEnoughBalanceToPayFees -> {
                resourceManager.getString(R.string.common_not_enough_funds_title) to
                    resourceManager.getString(R.string.common_not_enough_funds_message)
            }
        }
    }
}
