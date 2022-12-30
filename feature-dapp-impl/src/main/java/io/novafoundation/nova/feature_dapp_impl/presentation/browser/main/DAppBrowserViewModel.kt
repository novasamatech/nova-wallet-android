package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.filterWithPrevious
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_impl.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.browser.BrowserPage
import io.novafoundation.nova.feature_dapp_impl.domain.browser.BrowserPageAnalyzed
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DappBrowserInteractor
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsRequester
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignRequester
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.awaitConfirmation
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchRequester
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session.Authorization.State
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionStoreFactory
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.ExternalEvent
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3StateMachineHost
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.AuthorizeDAppPayload
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxRequest
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

enum class ConfirmationState {
    ALLOWED, REJECTED, CANCELLED
}

data class DesktopModeChangedEvent(val desktopModeEnabled: Boolean, val url: String)

class DAppBrowserViewModel(
    private val router: DAppRouter,
    private val signRequester: DAppSignRequester,
    private val extensionStoreFactory: ExtensionStoreFactory,
    private val interactor: DappBrowserInteractor,
    private val dAppSearchRequester: DAppSearchRequester,
    private val dAppOptionsRequester: DAppOptionsRequester,
    private val initialUrl: String,
    private val selectedAccountUseCase: SelectedAccountUseCase
) : BaseViewModel(), Web3StateMachineHost {

    private val _showConfirmationDialog = MutableLiveData<Event<DappPendingConfirmation<*>>>()
    val showConfirmationSheet = _showConfirmationDialog

    override val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .share()

    private val currentPage = singleReplaySharedFlow<BrowserPage>()

    override val currentPageAnalyzed = currentPage.flatMapLatest {
        interactor.observeBrowserPageFor(it)
    }.shareInBackground()

    override val externalEvents = singleReplaySharedFlow<ExternalEvent>()

    private val _browserNavigationCommandEvent = MutableLiveData<Event<BrowserNavigationCommand>>()
    val browserNavigationCommandEvent: LiveData<Event<BrowserNavigationCommand>> = _browserNavigationCommandEvent

    val extensionsStore = extensionStoreFactory.create(hostApi = this, coroutineScope = this)

    private val _userOptionDesktopMode = MutableStateFlow(false)

    val desktopModeChangedModel = combine(_userOptionDesktopMode, currentPageAnalyzed) { userDesktopMode, currentPage ->
        val isDesktopModeEnabled = userDesktopMode || currentPage.desktopOnly
        DesktopModeChangedEvent(isDesktopModeEnabled, currentPage.url)
    }
        .filterWithPrevious { old, new -> old?.desktopModeEnabled != new.desktopModeEnabled }
        .shareInBackground()

    init {
        dAppSearchRequester.responseFlow
            .onEach { it.newUrl?.let(::forceLoad) }
            .launchIn(this)

        watchDangerousWebsites()

        listenOptionsChange()

        forceLoad(initialUrl)
    }

    override suspend fun authorizeDApp(payload: AuthorizeDAppPayload): State {
        val confirmationState = awaitConfirmation(DappPendingConfirmation.Action.Authorize(payload))

        return mapConfirmationStateToAuthorizationState(confirmationState)
    }

    override suspend fun confirmTx(request: ConfirmTxRequest): ConfirmTxResponse {
        val response = withContext(Dispatchers.Main) {
            signRequester.awaitConfirmation(mapSignExtrinsicRequestToPayload(request))
        }

        return when (response) {
            is DAppSignCommunicator.Response.Rejected -> ConfirmTxResponse.Rejected(response.requestId)
            is DAppSignCommunicator.Response.Signed -> ConfirmTxResponse.Signed(response.requestId, response.signature)
            is DAppSignCommunicator.Response.SigningFailed -> ConfirmTxResponse.SigningFailed(response.requestId, response.shouldPresent)
            is DAppSignCommunicator.Response.Sent -> ConfirmTxResponse.Sent(response.requestId, response.txHash)
        }
    }

    override fun reloadPage() {
        _browserNavigationCommandEvent.postValue(BrowserNavigationCommand.Reload.event())
    }

    fun onPageChanged(url: String, title: String?) {
        updateCurrentPage(url, title, synchronizedWithBrowser = true)
    }

    fun closeClicked() = launch {
        val confirmationState = awaitConfirmation(DappPendingConfirmation.Action.CloseScreen)

        if (confirmationState == ConfirmationState.ALLOWED) {
            exitBrowser()
        }
    }

    fun openSearch() = launch {
        val currentPage = currentPage.first()

        dAppSearchRequester.openRequest(SearchPayload(initialUrl = currentPage.url))
    }

    fun onMoreClicked() {
        launch {
            val payload = getCurrentPageOptionsPayload()
            dAppOptionsRequester.openRequest(payload)
        }
    }

    private fun listenOptionsChange() {
        dAppOptionsRequester.responseFlow
            .onEach {
                when (it) {
                    DAppOptionsCommunicator.Response.DesktopModeClick -> _userOptionDesktopMode.toggle()
                }
            }
            .launchIn(this)
    }

    private fun watchDangerousWebsites() {
        currentPageAnalyzed
            .filter { it.synchronizedWithBrowser && it.security == BrowserPageAnalyzed.Security.DANGEROUS }
            .distinctUntilChanged()
            .onEach {
                externalEvents.emit(ExternalEvent.PhishingDetected)

                awaitConfirmation(DappPendingConfirmation.Action.AcknowledgePhishingAlert)

                exitBrowser()
            }
            .launchIn(this)
    }

    private fun forceLoad(url: String) {
        _browserNavigationCommandEvent.value = BrowserNavigationCommand.OpenUrl(url).event()

        updateCurrentPage(url, title = null, synchronizedWithBrowser = false)
    }

    private suspend fun getCurrentPageOptionsPayload(): DAppOptionsPayload {
        val page = currentPageAnalyzed.first()
        val currentPageTitle = page.title ?: page.display
        val isCurrentPageFavorite = page.isFavourite
        return DAppOptionsPayload(
            currentPageTitle,
            isCurrentPageFavorite,
            isDesktopModeEnabled = desktopModeChangedModel.first().desktopModeEnabled,
            isDesktopModeOnly = currentPageAnalyzed.first().desktopOnly,
            url = page.url
        )
    }

    private suspend fun awaitConfirmation(action: DappPendingConfirmation.Action) = suspendCoroutine<ConfirmationState> {
        val confirmation = DappPendingConfirmation(
            onConfirm = { it.resume(ConfirmationState.ALLOWED) },
            onDeny = { it.resume(ConfirmationState.REJECTED) },
            onCancel = { it.resume(ConfirmationState.CANCELLED) },
            action = action
        )

        _showConfirmationDialog.postValue(confirmation.event())
    }

    private fun mapConfirmationStateToAuthorizationState(
        confirmationState: ConfirmationState
    ): State = when (confirmationState) {
        ConfirmationState.ALLOWED -> State.ALLOWED
        ConfirmationState.REJECTED -> State.REJECTED
        ConfirmationState.CANCELLED -> State.NONE
    }

    private fun exitBrowser() = router.back()

    private fun updateCurrentPage(
        url: String,
        title: String?,
        synchronizedWithBrowser: Boolean
    ) = launch {
        currentPage.emit(BrowserPage(url, title, synchronizedWithBrowser))
    }

    private suspend fun mapSignExtrinsicRequestToPayload(request: ConfirmTxRequest) = DAppSignPayload(
        body = request,
        dappUrl = currentPageAnalyzed.first().url
    )
}
