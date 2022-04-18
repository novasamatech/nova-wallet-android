package io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.states

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.polkadotJs.PolkadotJsExtensionInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsTransportRequest
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.PolkadotJsSignPayload
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.model.SignerResult
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.ExternalEvent
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.StateMachineTransition
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3StateMachineHost
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3StateMachineHost.NotAuthorizedException
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.AuthorizeDAppPayload
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxRequest
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxResponse
import io.novafoundation.nova.feature_dapp_impl.web3.states.selectedMetaAccountId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

class DefaultPolkadotJsState(
    private val interactor: PolkadotJsExtensionInteractor,
    private val commonInteractor: DappInteractor,
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
    private val web3Session: Web3Session,
    private val hostApi: Web3StateMachineHost,
) : PolkadotJsState {

    override suspend fun acceptRequest(request: PolkadotJsTransportRequest<*>, transition: StateMachineTransition<PolkadotJsState>) {
        val authorizationState = web3Session.authorizationStateFor(request.url, hostApi.selectedMetaAccountId())

        when (request) {
            is PolkadotJsTransportRequest.Single.AuthorizeTab -> authorizeTab(request, authorizationState)
            is PolkadotJsTransportRequest.Single.ListAccounts -> supplyAccountList(request, authorizationState)
            is PolkadotJsTransportRequest.Single.Sign -> signExtrinsicIfAllowed(request, authorizationState)
            is PolkadotJsTransportRequest.Subscription.SubscribeAccounts -> supplyAccountListSubscription(request, authorizationState)
            is PolkadotJsTransportRequest.Single.ListMetadata -> suppleKnownMetadatas(request, authorizationState)
            is PolkadotJsTransportRequest.Single.ProvideMetadata -> handleProvideMetadata(request, authorizationState)
        }
    }

    override suspend fun acceptEvent(event: ExternalEvent, transition: StateMachineTransition<PolkadotJsState>) {
        when (event) {
            ExternalEvent.PhishingDetected -> transition.emitState(PhishingDetectedPolkadotJsState())
        }
    }

    private suspend fun authorizeTab(request: PolkadotJsTransportRequest.Single.AuthorizeTab, state: Web3Session.Authorization.State) {
        when (state) {
            // user already accepted - no need to ask second time
            Web3Session.Authorization.State.ALLOWED -> request.accept(PolkadotJsTransportRequest.Single.AuthorizeTab.Response(true))
            // first time dapp request authorization during this session
            Web3Session.Authorization.State.NONE -> authorizeTabWithConfirmation(request)
            // user rejected this dapp previosuly - ask for authorization one more time
            Web3Session.Authorization.State.REJECTED -> authorizeTabWithConfirmation(request)
        }
    }

    private suspend fun signExtrinsicIfAllowed(request: PolkadotJsTransportRequest.Single.Sign, state: Web3Session.Authorization.State) {
        when (state) {
            // request user confirmation if dapp is authorized
            Web3Session.Authorization.State.ALLOWED -> signExtrinsicWithConfirmation(request)
            // reject otherwise
            else -> request.reject(NotAuthorizedException)
        }
    }

    private suspend fun signExtrinsicWithConfirmation(request: PolkadotJsTransportRequest.Single.Sign) {
        val signRequest = ConfirmTxRequest(id = request.requestId, payload = PolkadotJsSignPayload(request.signerPayload))

        when (val response = hostApi.confirmTx(signRequest)) {
            is ConfirmTxResponse.Rejected -> request.reject(NotAuthorizedException)
            is ConfirmTxResponse.Sent -> throw IllegalStateException("Unexpected 'Sent' response for PolkadotJs extension")
            is ConfirmTxResponse.Signed -> request.accept(SignerResult(response.requestId, response.signature))
            is ConfirmTxResponse.SigningFailed -> {
                hostApi.showError(resourceManager.getString(R.string.dapp_sign_extrinsic_failed))

                request.reject(Web3StateMachineHost.SigningFailedException)
            }
        }
    }

    private suspend fun handleProvideMetadata(
        request: PolkadotJsTransportRequest.Single.ProvideMetadata,
        state: Web3Session.Authorization.State
    ) = respondIfAllowed(request, state) {
        false // we do not accept provided metadata since app handles metadata sync by its own
    }

    private suspend fun suppleKnownMetadatas(
        request: PolkadotJsTransportRequest.Single.ListMetadata,
        state: Web3Session.Authorization.State
    ) = respondIfAllowed(request, state) {
        interactor.getKnownInjectedMetadatas()
    }

    private suspend fun supplyAccountList(
        request: PolkadotJsTransportRequest.Single.ListAccounts,
        state: Web3Session.Authorization.State
    ) = respondIfAllowed(request, state) {
        interactor.getInjectedAccounts()
    }

    private suspend fun supplyAccountListSubscription(
        request: PolkadotJsTransportRequest.Subscription.SubscribeAccounts,
        state: Web3Session.Authorization.State
    ) = respondIfAllowed(request, state) {
        flowOf(interactor.getInjectedAccounts())
    }

    private suspend fun <T> respondIfAllowed(
        request: PolkadotJsTransportRequest<T>,
        state: Web3Session.Authorization.State,
        responseConstructor: suspend () -> T
    ) = if (state == Web3Session.Authorization.State.ALLOWED) {
        request.accept(responseConstructor())
    } else {
        request.reject(NotAuthorizedException)
    }

    private suspend fun authorizeTabWithConfirmation(request: PolkadotJsTransportRequest.Single.AuthorizeTab) {
        val currentPage = hostApi.currentPageAnalyzed.first()
        val selectedAccount = hostApi.selectedAccount.first()
        // use url got from browser instead of url got from dApp to prevent dApp supplying wrong URL
        val dAppInfo = commonInteractor.getDAppInfo(dAppUrl = currentPage.url)

        val dAppIdentifier = dAppInfo.metadata?.name ?: currentPage.title ?: dAppInfo.baseUrl

        val action = AuthorizeDAppPayload(
            title = resourceManager.getString(
                R.string.dapp_confirm_authorize_title_format,
                dAppIdentifier
            ),
            dAppIconUrl = dAppInfo.metadata?.iconLink,
            dAppUrl = dAppInfo.baseUrl,
            walletAddressModel = addressIconGenerator.createAddressModel(
                accountAddress = selectedAccount.defaultSubstrateAddress,
                sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
                accountName = selectedAccount.name
            )
        )

        val authorizationState = hostApi.authorizeDApp(action)

        web3Session.updateAuthorization(
            state = authorizationState,
            fullUrl = request.url,
            dAppTitle = dAppIdentifier,
            metaId = selectedAccount.id
        )

        val authorized = authorizationState == Web3Session.Authorization.State.ALLOWED

        request.accept(PolkadotJsTransportRequest.Single.AuthorizeTab.Response(authorized))
    }
}
