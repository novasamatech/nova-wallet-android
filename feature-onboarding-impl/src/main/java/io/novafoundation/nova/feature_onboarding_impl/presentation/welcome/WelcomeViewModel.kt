package io.novafoundation.nova.feature_onboarding_impl.presentation.welcome

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.analytics.AnalyticsEvent
import io.novafoundation.nova.common.data.analytics.AnalyticsService
import io.novafoundation.nova.common.data.analytics.OnboardingSource
import io.novafoundation.nova.common.data.analytics.WalletCreationMethod
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_onboarding_impl.OnboardingRouter
import io.novafoundation.nova.feature_versions_api.domain.UpdateNotificationsInteractor

class WelcomeViewModel(
    shouldShowBack: Boolean,
    private val router: OnboardingRouter,
    private val appLinksProvider: AppLinksProvider,
    private val addAccountPayload: AddAccountPayload,
    updateNotificationsInteractor: UpdateNotificationsInteractor,
    private val analyticsService: AnalyticsService
) : BaseViewModel(),
    Browserable {

    val shouldShowBackLiveData: LiveData<Boolean> = MutableLiveData(shouldShowBack)

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    init {
        updateNotificationsInteractor.allowInAppUpdateCheck()

        // Only track for add_wallet flow — fresh install can't fire (user hasn't consented to analytics yet)
        if (shouldShowBack) {
            analyticsService.track(AnalyticsEvent.OnboardingStarted(OnboardingSource.ADD_WALLET))
        }
    }

    fun createAccountClicked() {
        analyticsService.track(AnalyticsEvent.WalletCreationMethodSelected(WalletCreationMethod.CREATE))
        when (addAccountPayload) {
            is AddAccountPayload.MetaAccount -> router.openCreateFirstWallet()
            is AddAccountPayload.ChainAccount -> router.openMnemonicScreen(accountName = null, addAccountPayload)
        }
    }

    fun importAccountClicked() {
        analyticsService.track(AnalyticsEvent.WalletCreationMethodSelected(WalletCreationMethod.IMPORT_MNEMONIC))
        router.openImportOptionsScreen()
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
}
