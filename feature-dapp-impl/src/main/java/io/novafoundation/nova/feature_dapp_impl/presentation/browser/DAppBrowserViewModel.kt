package io.novafoundation.nova.feature_dapp_impl.presentation.browser

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
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DappBrowserInteractor
import io.novafoundation.nova.feature_dapp_impl.web3.Web3Session.AuthorizationState
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionFactory
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionRequest
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionRequest.AccountList
import io.novafoundation.nova.feature_dapp_impl.web3.polkadotJs.PolkadotJsExtensionRequest.AuthorizeTab
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object NotAuthorizedException : Exception("Rejected by user")

class DAppBrowserViewModel(
    private val router: DAppRouter,
    private val polkadotJsExtensionFactory: PolkadotJsExtensionFactory,
    private val interactor: DappBrowserInteractor,
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : BaseViewModel() {

    private val polkadotJsExtension = polkadotJsExtensionFactory.create(scope = this)

    private val _showConfirmationSheet = MutableLiveData<Event<DappPendingConfirmation<*>>>()
    val showConfirmationSheet = _showConfirmationSheet

    private val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .share()

    init {
        polkadotJsExtension.requestsFlow
            .onEach(::handleDAppRequest)
            .inBackground()
            .launchIn(this)
    }

    private suspend fun handleDAppRequest(it: PolkadotJsExtensionRequest<*>) {
        val authorizationState = polkadotJsExtension.session.authorizationStateFor(it.url)

        if (authorizationState == AuthorizationState.REJECTED) {
            it.reject(NotAuthorizedException)
            return
        }

        when (it) {
            is AuthorizeTab -> authorizeTab(it)
            is AccountList -> supplyAccountList(it)
        }
    }

    private suspend fun authorizeTab(request: AuthorizeTab) {
        val dappInfo = interactor.getDAppInfo(request.url)

        val dAppIdentifier = dappInfo.metadata?.name ?: dappInfo.baseUrl

        val metaAccount = selectedAccount.first()

        val action = DappPendingConfirmation.Action.Authorize(
            title = resourceManager.getString(R.string.dapp_confirm_authorize_title_format, dAppIdentifier),
            dAppIconUrl = dappInfo.metadata?.iconLink,
            dAppUrl = dappInfo.baseUrl,
            walletAddressModel = addressIconGenerator.createAddressModel(
                accountAddress = metaAccount.defaultSubstrateAddress,
                sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
                accountName = metaAccount.name
            )
        )

        val authorizationState = awaitConfirmation(action)

        polkadotJsExtension.session.updateAuthorizationState(request.url, authorizationState)

        request.accept(AuthorizeTab.Response(authorizationState == AuthorizationState.ALLOWED))
    }

    private suspend fun supplyAccountList(request: AccountList) {
        val injectedAccounts = interactor.getInjectedAccounts()

        request.accept(AccountList.Response(injectedAccounts))
    }

    private suspend fun awaitConfirmation(action: DappPendingConfirmation.Action) = suspendCoroutine<AuthorizationState> {
        val confirmation = DappPendingConfirmation(
            onConfirm = { it.resume(AuthorizationState.ALLOWED) },
            onDeny = { it.resume(AuthorizationState.REJECTED) },
            onCancel = { it.resume(AuthorizationState.NONE) },
            action = action
        )

        _showConfirmationSheet.postValue(confirmation.event())
    }
}
