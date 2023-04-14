package io.novafoundation.nova.feature_account_impl.presentation.settings

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
import io.novafoundation.nova.common.sequrity.TwoFactorVerificationService
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.language.mapper.mapLanguageToLanguageModel
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import io.novafoundation.nova.feature_currency_api.presentation.mapper.mapCurrencyToUI
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val accountInteractor: AccountInteractor,
    private val router: AccountRouter,
    private val appLinksProvider: AppLinksProvider,
    private val resourceManager: ResourceManager,
    private val appVersionProvider: AppVersionProvider,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val currencyInteractor: CurrencyInteractor,
    private val safeModeService: SafeModeService,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val twoFactorVerificationService: TwoFactorVerificationService
) : BaseViewModel(), Browserable {

    val confirmationAwaitableAction = actionAwaitableMixinFactory.confirmingAction<SettingsConfirmationData>()

    val selectedWalletModel = selectedAccountUseCase.selectedWalletModelFlow()
        .shareInBackground()

    val selectedCurrencyFlow = currencyInteractor.observeSelectCurrency()
        .map { mapCurrencyToUI(it) }
        .inBackground()
        .share()

    val selectedLanguageFlow = flowOf {
        val language = accountInteractor.getSelectedLanguage()

        mapLanguageToLanguageModel(language)
    }
        .inBackground()
        .share()

    val appVersionFlow = flowOf {
        resourceManager.getString(R.string.about_version_template, appVersionProvider.versionName)
    }
        .inBackground()
        .share()

    val pinCodeVerificationStatus = twoFactorVerificationService.stateFlow()

    val safeModeStatus = safeModeService.safeModeStatusFlow()

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    private val _openEmailEvent = MutableLiveData<Event<String>>()
    val openEmailEvent: LiveData<Event<String>> = _openEmailEvent

    fun walletsClicked() {
        router.openWallets()
    }

    fun networksClicked() {
        router.openNodes()
    }

    fun currenciesClicked() {
        router.openCurrencies()
    }

    fun languagesClicked() {
        router.openLanguages()
    }

    fun changePincodeVerification() {
        launch {
            if (!twoFactorVerificationService.isEnabled()) {
                confirmationAwaitableAction.awaitAction(
                    SettingsConfirmationData(
                        R.string.settings_pin_code_verification_confirmation_title,
                        R.string.settings_pin_code_verification_confirmation_message
                    )
                )
            }

            twoFactorVerificationService.toggle()
        }
    }

    fun changeSafeMode() {
        launch {
            if (!safeModeService.isSafeModeEnabled()) {
                confirmationAwaitableAction.awaitAction(
                    SettingsConfirmationData(
                        R.string.settings_safe_mode_confirmation_title,
                        R.string.settings_safe_mode_confirmation_message
                    )
                )
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

    private fun openLink(link: String) {
        openBrowserEvent.value = link.event()
    }
}
