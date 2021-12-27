package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DappBrowserInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignExtrinsicRequester
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.awaitConfirmation
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Session.AuthorizationState
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionRequest
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionRequest.AccountList
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionRequest.AuthorizeTab
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionRequest.SignExtrinsic
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object NotAuthorizedException : Exception("Rejected by user")
object SigningFailedException : Exception("Signing failed")

enum class ConfirmationState {
    ALLOWED, REJECTED, CANCELLED
}

class DAppBrowserViewModel(
    private val router: DAppRouter,
    private val signExtrinsicRequester: DAppSignExtrinsicRequester,
    private val polkadotJsExtensionFactory: PolkadotJsExtensionFactory,
    private val interactor: DappBrowserInteractor,
    private val commonInteractor: DappInteractor,
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : BaseViewModel() {

    private val polkadotJsExtension = polkadotJsExtensionFactory.create(scope = this)

    private val _showConfirmationSheet = MutableLiveData<Event<DappPendingConfirmation<*>>>()
    val showConfirmationSheet = _showConfirmationSheet

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .share()

    private val _loadUrlEvent = MutableLiveData<Event<String>>()
    val loadUrlEvent: LiveData<Event<String>> = _loadUrlEvent

    init {
        polkadotJsExtension.requestsFlow
            .onEach(::handleDAppRequest)
            .inBackground()
            .launchIn(this)

        _loadUrlEvent.value = "https://polkadot.js.org/apps/?rpc=wss%3A%2F%2Fwestend.api.onfinality.io%2Fpublic-ws#/accounts".event()
    }

    private suspend fun handleDAppRequest(request: PolkadotJsExtensionRequest<*>) {
        val authorizationState = polkadotJsExtension.session.authorizationStateFor(request.url)

        when (request) {
            is AuthorizeTab -> authorizeTab(request, authorizationState)
            is AccountList -> supplyAccountList(request, authorizationState)
            is SignExtrinsic -> signExtrinsicIfAllowed(request, authorizationState)
        }
    }

    private suspend fun signExtrinsicIfAllowed(request: SignExtrinsic, authorizationState: AuthorizationState) {
        when (authorizationState) {
            // request user confirmation if dapp is authorized
            AuthorizationState.ALLOWED -> signExtrinsicWithConfirmation(request)
            // reject otherwise
            else -> request.reject(NotAuthorizedException)
        }
    }

    private suspend fun signExtrinsicWithConfirmation(request: SignExtrinsic) {
        val response = withContext(Dispatchers.Main) {
            signExtrinsicRequester.awaitConfirmation(mapSignExtrinsicRequestToPayload(request))
        }

        when (response) {
            is DAppSignExtrinsicCommunicator.Response.Rejected -> request.reject(
                NotAuthorizedException
            )
            is DAppSignExtrinsicCommunicator.Response.Signed -> request.accept(SignerResult(response.requestId, response.signature))
            is DAppSignExtrinsicCommunicator.Response.SigningFailed -> {
                showError(resourceManager.getString(R.string.dapp_sign_extrinsic_failed))

                request.reject(SigningFailedException)
            }
        }
    }

    private suspend fun authorizeTab(request: AuthorizeTab, authorizationState: AuthorizationState) {
        when (authorizationState) {
            // user already accepted - no need to ask second time
            AuthorizationState.ALLOWED -> request.accept(AuthorizeTab.Response(true))
            // first time dapp request authorization during this session
            AuthorizationState.NONE -> authorizeTabWithConfirmation(request)
            // user rejected this dapp - automatically reject next authorization requests
            AuthorizationState.REJECTED -> request.reject(NotAuthorizedException)
        }
    }

    private suspend fun authorizeTabWithConfirmation(request: AuthorizeTab) {
        val dappInfo = commonInteractor.getDAppInfo(request.url)

        val dAppIdentifier = dappInfo.metadata?.name ?: dappInfo.baseUrl

        val metaAccount = selectedAccount.first()

        val action = DappPendingConfirmation.Action.Authorize(
            title = resourceManager.getString(
                R.string.dapp_confirm_authorize_title_format,
                dAppIdentifier
            ),
            dAppIconUrl = dappInfo.metadata?.iconLink,
            dAppUrl = dappInfo.baseUrl,
            walletAddressModel = addressIconGenerator.createAddressModel(
                accountAddress = metaAccount.defaultSubstrateAddress,
                sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
                accountName = metaAccount.name
            )
        )

        val confirmationState = awaitConfirmation(action)

        val authorizationState = mapConfirmationStateToAuthorizationState(confirmationState)

        polkadotJsExtension.session.updateAuthorizationState(request.url, authorizationState)

        request.accept(AuthorizeTab.Response(authorizationState == AuthorizationState.ALLOWED))
    }

    private suspend fun supplyAccountList(
        request: AccountList,
        authorizationState: AuthorizationState
    ) = respondIfAllowed(request, authorizationState) {
        val injectedAccounts = interactor.getInjectedAccounts()

        AccountList.Response(injectedAccounts)
    }

    private suspend fun awaitConfirmation(action: DappPendingConfirmation.Action) = suspendCoroutine<ConfirmationState> {
        val confirmation = DappPendingConfirmation(
            onConfirm = { it.resume(ConfirmationState.ALLOWED) },
            onDeny = { it.resume(ConfirmationState.REJECTED) },
            onCancel = { it.resume(ConfirmationState.CANCELLED) },
            action = action
        )

        _showConfirmationSheet.postValue(confirmation.event())
    }

    private fun mapConfirmationStateToAuthorizationState(
        confirmationState: ConfirmationState
    ): AuthorizationState = when (confirmationState) {
        ConfirmationState.ALLOWED -> AuthorizationState.ALLOWED
        ConfirmationState.REJECTED -> AuthorizationState.REJECTED
        ConfirmationState.CANCELLED -> AuthorizationState.NONE
    }

    private suspend fun <T> respondIfAllowed(
        request: PolkadotJsExtensionRequest<T>,
        authorizationState: AuthorizationState,
        responseConstructor: suspend () -> T
    ) = if (authorizationState == AuthorizationState.ALLOWED) {
        request.accept(responseConstructor())
    } else {
        request.reject(NotAuthorizedException)
    }

    private fun mapSignExtrinsicRequestToPayload(request: SignExtrinsic) = DAppSignExtrinsicPayload(
        requestId = request.requestId,
        signerPayloadJSON = request.signerPayload,
        dappUrl = request.url
    )
}
