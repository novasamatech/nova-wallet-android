package io.novafoundation.nova.app.root.presentation

import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.app.R
import io.novafoundation.nova.app.root.domain.RootInteractor
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.api.NetworkStateMixin
import io.novafoundation.nova.common.mixin.api.NetworkStateUi
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver
import io.novafoundation.nova.common.utils.sequrity.BackgroundAccessObserver.State.REQUEST_ACCESS
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.runtime.multiNetwork.connection.ChainConnection.ExternalRequirement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
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
    private val backgroundAccessObserver: BackgroundAccessObserver
) : BaseViewModel(), NetworkStateUi by networkStateMixin {

    private var willBeClearedForLanguageChange = false

    init {
        contributionsInteractor.runUpdate()
            .launchIn(this)

        interactor.runBalancesUpdate()
            .onEach { handleUpdatesSideEffect(it) }
            .launchIn(this)

        backgroundAccessObserver.stateFlow
            .filter { it == REQUEST_ACCESS }
            .onEach { verifyUserIfNeed() }
            .launchIn(this)

        syncCurrencies()

        updatePhishingAddresses()
    }

    private fun syncCurrencies() {
        launch { currencyInteractor.syncCurrencies() }
    }

    private fun handleUpdatesSideEffect(sideEffect: Updater.SideEffect) {
        // pass
    }

    private fun updatePhishingAddresses() {
        viewModelScope.launch {
            interactor.updatePhishingAddresses()
        }
    }

    fun noticeInBackground() {
        if (!willBeClearedForLanguageChange) {
            externalConnectionRequirementFlow.value = ExternalRequirement.STOPPED
        }
    }

    fun noticeInForeground() {
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

    fun externalUrlOpened(uri: String) {
        if (interactor.isBuyProviderRedirectLink(uri)) {
            showMessage(resourceManager.getString(R.string.buy_completed))
        }
    }

    private fun verifyUserIfNeed() {
        launch {
            if (interactor.isAccountSelected() && interactor.isPinCodeSet()) {
                rootRouter.nonCancellableVerify()
            } else {
                backgroundAccessObserver.onAccessed()
            }
        }
    }
}
