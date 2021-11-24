package io.novafoundation.nova.feature_account_impl.presentation.settings

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.AppVersionProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.list.AccountChosenNavDirection
import io.novafoundation.nova.feature_account_impl.presentation.language.mapper.mapLanguageToLanguageModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val interactor: AccountInteractor,
    private val router: AccountRouter,
    private val appLinksProvider: AppLinksProvider,
    private val resourceManager: ResourceManager,
    private val appVersionProvider: AppVersionProvider,
    private val addressIconGenerator: AddressIconGenerator,
) : BaseViewModel(), Browserable {

    val selectedAccountFlow = interactor.selectedMetaAccountFlow()
        .inBackground()
        .share()

    val accountIconFlow = selectedAccountFlow.map {
        addressIconGenerator.createAddressIcon(it.substrateAccountId, AddressIconGenerator.SIZE_BIG)
    }
        .inBackground()
        .share()

    val selectedLanguageFlow = flowOf {
        val language = interactor.getSelectedLanguage()

        mapLanguageToLanguageModel(language)
    }
        .inBackground()
        .share()

    val appVersionFlow = flowOf {
        resourceManager.getString(R.string.about_version_template, appVersionProvider.versionName)
    }
        .inBackground()
        .share()

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    fun walletsClicked() {
        router.openAccounts(AccountChosenNavDirection.MAIN)
    }

    fun networksClicked() {
        router.openNodes()
    }

    fun languagesClicked() {
        router.openLanguages()
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

    fun accountActionsClicked() = launch {
        router.openAccountDetails(selectedAccountFlow.first().id)
    }

    private fun openLink(link: String) {
        openBrowserEvent.value = Event(link)
    }
}
