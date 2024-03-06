package io.novafoundation.nova.app.root.presentation

import android.net.Uri
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.app.root.presentation.deepLinks.common.DeepLinkHandlingException
import io.novafoundation.nova.app.root.presentation.deepLinks.common.formatDeepLinkHandlingException
import io.novafoundation.nova.app.root.presentation.requestBusHandler.CompoundRequestBusHandler
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.NetworkStateMixin
import io.novafoundation.nova.common.mixin.api.NetworkStateUi
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_deep_linking.presentation.handling.CallbackEvent
import io.novafoundation.nova.feature_deep_linking.presentation.handling.DeepLinkHandler
import io.novafoundation.nova.feature_push_notifications.data.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import io.novafoundation.nova.feature_wallet_connect_api.presentation.WalletConnectService
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection.ExternalRequirement
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class RootViewModel(
    private val interactor: RootInteractor,
    private val currencyInteractor: CurrencyInteractor,
    private val rootRouter: RootRouter,
    private val externalConnectionRequirementFlow: MutableStateFlow<ExternalRequirement>,
    private val resourceManager: ResourceManager,
    private val networkStateMixin: NetworkStateMixin,
    private val contributionsInteractor: ContributionsInteractor,
    private val backgroundAccessObserver: BackgroundAccessObserver,
    private val safeModeService: SafeModeService,
    private val updateNotificationsInteractor: UpdateNotificationsInteractor,
    private val walletConnectService: WalletConnectService,
    private val walletConnectSessionsUseCase: WalletConnectSessionsUseCase,
    private val deepLinkHandler: DeepLinkHandler,
    private val rootScope: RootScope,
    private val compoundRequestBusHandler: CompoundRequestBusHandler,
    private val pushNotificationsInteractor: PushNotificationsInteractor
) : BaseViewModel(), NetworkStateUi by networkStateMixin {

    private var willBeClearedForLanguageChange = false

    init {
        contributionsInteractor.runUpdate()
            .launchIn(this)

        interactor.runBalancesUpdate()
            .onEach { handleUpdatesSideEffect(it) }
            .launchIn(this)

        backgroundAccessObserver.requestAccessFlow
            .onEach { verifyUserIfNeed() }
            .launchIn(this)

        checkForUpdates()

        syncProxies()

        syncCurrencies()

        syncWalletConnectSessions()

        updatePhishingAddresses()

        obserBusEvents()

        walletConnectService.onPairErrorLiveData.observeForever {
            showError(it.peekContent())
        }

        subscribeDeepLinkCallback()

        syncPushSettingsIfNeeded()
    }

    private fun obserBusEvents() {
        compoundRequestBusHandler.observe()
    }

    private fun subscribeDeepLinkCallback() {
        deepLinkHandler.callbackFlow
            .onEach { handleDeepLinkCallbackEvent(it) }
            .launchIn(this)
    }

    private fun handleDeepLinkCallbackEvent(event: CallbackEvent) {
        when (event) {
            is CallbackEvent.Message -> {
                showMessage(event.message)
            }
        }
    }

    private fun syncWalletConnectSessions() = launch {
        walletConnectSessionsUseCase.syncActiveSessions()
    }

    private fun checkForUpdates() {
        launch {
            updateNotificationsInteractor.loadVersions()
            updateNotificationsInteractor.waitPermissionToUpdate()
            if (updateNotificationsInteractor.hasImportantUpdates()) {
                rootRouter.openUpdateNotifications()
            }
        }
    }

    private fun syncCurrencies() {
        launch { currencyInteractor.syncCurrencies() }
    }

    private fun syncProxies() {
        interactor.syncProxies()
            .inBackground()
            .launchIn(rootScope)
    }

    private fun handleUpdatesSideEffect(sideEffect: Updater.SideEffect) {
        // pass
    }

    private fun updatePhishingAddresses() {
        viewModelScope.launch {
            interactor.updatePhishingAddresses()
        }
    }

    private fun syncPushSettingsIfNeeded() {
        launch {
            pushNotificationsInteractor.syncSettings()
        }
    }

    fun noticeInBackground() {
        if (!willBeClearedForLanguageChange) {
            externalConnectionRequirementFlow.value = ExternalRequirement.STOPPED

            walletConnectService.disconnect()
        }
    }

    fun noticeInForeground() {
        walletConnectService.connect()

        externalConnectionRequirementFlow.value = ExternalRequirement.ALLOWED
    }

    fun noticeLanguageLanguage() {
        willBeClearedForLanguageChange = true
    }

    fun restoredAfterConfigChange() {
        if (willBeClearedForLanguageChange) {
            rootRouter.returnToWallet()

            willBeClearedForLanguageChange = false
        }
    }

    private fun verifyUserIfNeed() {
        launch {
            if (interactor.isAccountSelected() && interactor.isPinCodeSet()) {
                rootRouter.nonCancellableVerify()
            } else {
                backgroundAccessObserver.checkPassed()
            }
        }
    }

    fun applySafeModeIfEnabled() {
        safeModeService.applySafeModeIfEnabled()
    }

    override fun onCleared() {
        rootScope.cancel()
    }

    fun handleDeepLink(data: Uri) {
        launch {
            try {
                deepLinkHandler.handleDeepLink(data)
            } catch (e: DeepLinkHandlingException) {
                val errorMessage = formatDeepLinkHandlingException(resourceManager, e)
                showError(errorMessage)
            }
        }
    }
}
