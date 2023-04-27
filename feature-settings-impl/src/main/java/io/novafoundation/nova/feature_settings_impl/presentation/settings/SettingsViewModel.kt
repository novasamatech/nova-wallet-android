package io.novafoundation.nova.feature_settings_impl.presentation.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.SafeModeService
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageUseCase
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.presentation.mapper.mapCurrencyToUI
import io.novafoundation.nova.feature_settings_impl.R
import io.novafoundation.nova.feature_settings_impl.SettingsRouter
import io.novafoundation.nova.feature_settings_impl.presentation.settings.model.WalletConnectSessionsModel
import io.novafoundation.nova.feature_wallet_connect_api.domain.sessions.WalletConnectSessionsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val languageUseCase: LanguageUseCase,
    private val router: SettingsRouter,
    private val appLinksProvider: AppLinksProvider,
    private val resourceManager: ResourceManager,
    private val appVersionProvider: AppVersionProvider,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val currencyInteractor: CurrencyInteractor,
    private val safeModeService: SafeModeService,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val walletConnectSessionsUseCase: WalletConnectSessionsUseCase,
) : BaseViewModel(), Browserable {

    val safeModeAwaitableAction = actionAwaitableMixinFactory.confirmingAction<Unit>()

    val selectedWalletModel = selectedAccountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    val selectedCurrencyFlow = currencyInteractor.observeSelectCurrency()
        .map { mapCurrencyToUI(it) }
        .inBackground()
        .share()

    val selectedLanguageFlow = flowOf {
        languageUseCase.selectedLanguageModel()
    }
        .shareInBackground()

    val appVersionFlow = flowOf {
        resourceManager.getString(R.string.about_version_template, appVersionProvider.versionName)
    }
        .inBackground()
        .share()

    private val walletConnectSessions = walletConnectSessionsUseCase.activeSessionsNumberFlow()
        .shareInBackground()

    val walletConnectSessionsUi = walletConnectSessions
        .map(::mapNumberOfActiveSessionsToUi)
        .shareInBackground()

    val safeModeStatus = safeModeService.safeModeStatusFlow()

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val _openEmailEvent = MutableLiveData<Event<String>>()
    val openEmailEvent: LiveData<Event<String>> = _openEmailEvent

    init {
        syncWalletConnectSessions()
    }

    fun walletsClicked() {
        router.openWallets()
    }

    fun currenciesClicked() {
        router.openCurrencies()
    }

    fun languagesClicked() {
        router.openLanguages()
    }

    fun changeSafeMode() {
        launch {
            if (!safeModeService.isSafeModeEnabled()) {
                safeModeAwaitableAction.awaitAction()
            }

            safeModeService.toggleSafeMode()
        }
    }

    fun changePinCodeClicked() {
        router.openChangePinCode()
    }

    fun telegramClicked() {
        openLink(appLinksProvider.telegram)
    }

    fun twitterClicked() {
        openLink(appLinksProvider.twitter)
    }

    fun rateUsClicked() {
        openLink(appLinksProvider.rateApp)
    }

    fun websiteClicked() {
        openLink(appLinksProvider.website)
    }

    fun githubClicked() {
        openLink(appLinksProvider.github)
    }

    fun termsClicked() {
        openLink(appLinksProvider.termsUrl)
    }

    fun privacyClicked() {
        openLink(appLinksProvider.privacyUrl)
    }

    fun emailClicked() {
        _openEmailEvent.value = appLinksProvider.email.event()
    }

    fun openYoutube() {
        openLink(appLinksProvider.youtube)
    }

    fun accountActionsClicked() = launch {
        val selectedWalletId = selectedAccountUseCase.getSelectedMetaAccount().id

        router.openAccountDetails(selectedWalletId)
    }

    fun selectedWalletClicked() {
        router.openSwitchWallet()
    }

    fun walletConnectClicked() = launch {
        if (walletConnectSessions.first() > 0) {
            router.openWalletConnectSessions()
        } else {
            router.openWalletConnectScan()
        }
    }

    private fun openLink(link: String) {
        openBrowserEvent.value = link.event()
    }

    private fun mapNumberOfActiveSessionsToUi(activeSessions: Int): WalletConnectSessionsModel {
        return if (activeSessions > 0) {
            WalletConnectSessionsModel(activeSessions.format())
        } else {
            WalletConnectSessionsModel(null)
        }
    }

    private fun syncWalletConnectSessions() = launch {
        walletConnectSessionsUseCase.syncActiveSessions()
    }
}
