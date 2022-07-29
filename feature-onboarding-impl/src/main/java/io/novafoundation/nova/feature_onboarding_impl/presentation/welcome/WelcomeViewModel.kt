package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter

class WelcomeViewModel(
    shouldShowBack: Boolean,
    private val router: OnboardingRouter,
    private val appLinksProvider: AppLinksProvider,
    private val addAccountPayload: AddAccountPayload,
    private val importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
) : BaseViewModel(),
    ImportTypeChooserMixin by importTypeChooserMixin,
    Browserable {

    val shouldShowBackLiveData: LiveData<Boolean> = MutableLiveData(shouldShowBack)

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    fun createAccountClicked() {
        when (addAccountPayload) {
            is AddAccountPayload.MetaAccount -> router.openCreateAccount(addAccountPayload)
            is AddAccountPayload.ChainAccount -> router.openMnemonicScreen(accountName = null, addAccountPayload)
        }
    }

    fun importAccountClicked() {
        val payload = ImportTypeChooserMixin.Payload(
            onChosen = { router.openImportAccountScreen(ImportAccountPayload(it, addAccountPayload)) }
        )

        importTypeChooserMixin.showChooser(payload)
    }

    fun termsClicked() {
        openBrowserEvent.value = Event(appLinksProvider.termsUrl)
    }

    fun privacyClicked() {
        openBrowserEvent.value = Event(appLinksProvider.privacyUrl)
    }

    fun backClicked() {
        router.back()
    }

    fun addWatchWalletClicked() {
        router.openCreateWatchWallet()
    }
}
