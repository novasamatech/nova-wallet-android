package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.Urls
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.removeHexPrefix
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_dapp_api.data.model.BrowserHostSettings
import io.novafoundation.nova.feature_dapp_api.DAppRouter
import io.novafoundation.nova.feature_dapp_impl.domain.DappInteractor
import io.novafoundation.nova.feature_dapp_impl.domain.browser.BrowserPage
import io.novafoundation.nova.feature_dapp_impl.domain.browser.BrowserPageAnalyzed
import io.novafoundation.nova.feature_dapp_impl.domain.browser.DappBrowserInteractor
import io.novafoundation.nova.feature_dapp_api.presentation.addToFavorites.AddToFavouritesPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.options.DAppOptionsPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.common.favourites.RemoveFavouritesPayload
import io.novafoundation.nova.feature_dapp_impl.presentation.search.DAppSearchRequester
import io.novafoundation.nova.feature_dapp_impl.presentation.search.SearchPayload
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.BrowserTabPoolService
import io.novafoundation.nova.feature_dapp_impl.utils.tabs.models.CurrentTabState
import io.novafoundation.nova.feature_dapp_impl.web3.session.Web3Session.Authorization.State
import io.novafoundation.nova.feature_dapp_impl.web3.states.ExtensionStoreFactory
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3ExtensionStateMachine.ExternalEvent
import io.novafoundation.nova.feature_dapp_impl.web3.states.Web3StateMachineHost
import io.novafoundation.nova.feature_dapp_impl.web3.states.hostApi.ConfirmTxResponse
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignRequester
import io.novafoundation.nova.feature_external_sign_api.model.awaitConfirmation
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignPayload
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignRequest
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.ExternalSignWallet
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.SigningDappMetadata
import io.novafoundation.nova.feature_external_sign_api.model.signPayload.polkadot.genesisHash
import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet
import io.novafoundation.nova.runtime.ext.isDisabled
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chainsById
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
    private val signRequester: ExternalSignRequester,
    private val extensionStoreFactory: ExtensionStoreFactory,
    private val dAppInteractor: DappInteractor,
    private val interactor: DappBrowserInteractor,
    private val dAppSearchRequester: DAppSearchRequester,
    private val initialUrl: String,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val chainRegistry: ChainRegistry,
    private val browserTabPoolService: BrowserTabPoolService
) : BaseViewModel(), Web3StateMachineHost {

    val removeFromFavouritesConfirmation = actionAwaitableMixinFactory.confirmingAction<RemoveFavouritesPayload>()

    private val _showConfirmationDialog = MutableLiveData<Event<DappPendingConfirmation<*>>>()
    val showConfirmationSheet = _showConfirmationDialog

    override val selectedAccount = selectedAccountUseCase.selectedMetaAccountFlow()
        .share()

    private val currentPage = singleReplaySharedFlow<BrowserPage>()

    override val currentPageAnalyzed = currentPage.flatMapLatest {
        interactor.observeBrowserPageFor(it)
    }.shareInBackground()

    override val externalEvents = singleReplaySharedFlow<ExternalEvent>()

    private val _browserCommandEvent = MutableLiveData<Event<BrowserCommand>>()
    val browserCommandEvent: LiveData<Event<BrowserCommand>> = _browserCommandEvent

    private val _openBrowserOptionsEvent = MutableLiveData<Event<DAppOptionsPayload>>()
    val openBrowserOptionsEvent: LiveData<Event<DAppOptionsPayload>> = _openBrowserOptionsEvent

    val extensionsStore = extensionStoreFactory.create(hostApi = this, coroutineScope = this)

    private val isDesktopModeEnabledFlow = MutableStateFlow(false)

    val desktopModeChangedModel = currentPageAnalyzed
        .map { currentPage ->
            val hostSettings = interactor.getHostSettings(currentPage.url)
            val isDesktopModeEnabled = hostSettings?.isDesktopModeEnabled ?: isDesktopModeEnabledFlow.first()
            DesktopModeChangedEvent(isDesktopModeEnabled, currentPage.url)
        }
        .distinctUntilChanged()
        .shareInBackground()

    val currentTabFlow = browserTabPoolService.tabStateFlow
        .map { it.selectedTab }
        .filterIsInstance<CurrentTabState.Selected>()
        .shareInBackground()

    init {
        dAppSearchRequester.responseFlow
            .onEach { it.newUrl?.let(::forceLoad) }
            .launchIn(this)

        watchDangerousWebsites()

        launch {
            // TODO: We should create tab before this screen open
            browserTabPoolService.createNewTabAsCurrentTab(initialUrl)
        }
    }

    override suspend fun authorizeDApp(payload: AuthorizeDappBottomSheet.Payload): State {
        val confirmationState = awaitConfirmation(DappPendingConfirmation.Action.Authorize(payload))

        return mapConfirmationStateToAuthorizationState(confirmationState)
    }

    override suspend fun confirmTx(request: ExternalSignRequest): ConfirmTxResponse {
        val chainId = request.extractChainId()
        val chain = chainRegistry.chainsById()[chainId]

        if (chain != null && chain.isDisabled) {
            return ConfirmTxResponse.ChainIsDisabled(request.id, chain.name)
        }

        val response = withContext(Dispatchers.Main) {
            signRequester.awaitConfirmation(mapSignExtrinsicRequestToPayload(request))
        }

        return when (response) {
            is ExternalSignCommunicator.Response.Rejected -> ConfirmTxResponse.Rejected(response.requestId)
            is ExternalSignCommunicator.Response.Signed -> ConfirmTxResponse.Signed(response.requestId, response.signature, response.modifiedTransaction)
            is ExternalSignCommunicator.Response.SigningFailed -> ConfirmTxResponse.SigningFailed(response.requestId, response.shouldPresent)
            is ExternalSignCommunicator.Response.Sent -> ConfirmTxResponse.Sent(response.requestId, response.txHash)
        }
    }

    override fun reloadPage() {
        _browserCommandEvent.postValue(BrowserCommand.Reload.event())
    }

    fun detachCurrentSession() {
        browserTabPoolService.detachCurrentSession()
    }

    fun onPageChanged(url: String, title: String?) {
        updateCurrentPage(url, title, synchronizedWithBrowser = true)
    }

    fun closeClicked() = launch {
        exitBrowser()
    }

    fun openSearch() = launch {
        val currentPage = currentPage.first()

        dAppSearchRequester.openRequest(SearchPayload(initialUrl = currentPage.url))
    }

    fun onMoreClicked() {
        launch {
            val payload = getCurrentPageOptionsPayload()
            _openBrowserOptionsEvent.value = Event(payload)
        }
    }

    fun onFavoriteClick(optionsPayload: DAppOptionsPayload) {
        launch {
            if (optionsPayload.isFavorite) {
                removeFromFavouritesConfirmation.awaitAction(optionsPayload.currentPageTitle)

                dAppInteractor.removeDAppFromFavourites(optionsPayload.url)
            } else {
                val payload = AddToFavouritesPayload(
                    url = optionsPayload.url,
                    label = optionsPayload.currentPageTitle,
                    iconLink = null
                )

                router.openAddToFavourites(payload)
            }
        }
    }

    fun onDesktopClick() {
        launch {
            val desktopModeChangedEvent = desktopModeChangedModel.first()
            val newDesktopMode = !desktopModeChangedEvent.desktopModeEnabled
            val settings = BrowserHostSettings(Urls.normalizeUrl(desktopModeChangedEvent.url), newDesktopMode)
            interactor.saveHostSettings(settings)
            isDesktopModeEnabledFlow.value = newDesktopMode
            _browserCommandEvent.postValue(BrowserCommand.ChangeDesktopMode(newDesktopMode).event())
        }
    }

    fun openTabs() {
        router.openTabs()
    }

    fun makePageSnapshot() = launch {
        browserTabPoolService.makeCurrentTabSnapshot()
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
        _browserCommandEvent.value = BrowserCommand.OpenUrl(url).event()

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

    private suspend fun mapSignExtrinsicRequestToPayload(request: ExternalSignRequest): ExternalSignPayload {
        return ExternalSignPayload(
            signRequest = request,
            dappMetadata = getDAppSignMetadata(currentPageAnalyzed.first().url),
            wallet = ExternalSignWallet.Current
        )
    }

    private suspend fun getDAppSignMetadata(dAppUrl: String): SigningDappMetadata {
        val dappMetadata = dAppInteractor.getDAppInfo(dAppUrl)

        return SigningDappMetadata(
            icon = dappMetadata.metadata?.iconLink,
            name = dappMetadata.metadata?.name,
            url = dappMetadata.baseUrl,
        )
    }

    private fun ExternalSignRequest.extractChainId(): String? {
        return when (this) {
            is ExternalSignRequest.Evm -> null
            is ExternalSignRequest.Polkadot -> payload.genesisHash()?.removeHexPrefix()
        }
    }
}
