package io.novafoundation.nova.feature_account_impl.presentation.about

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.BuildConfig
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter

class AboutViewModel(
    private val router: AccountRouter,
    context: Context,
    private val appLinksProvider: AppLinksProvider,
    resourceManager: ResourceManager
) : BaseViewModel(), Browserable {

    private val _websiteLiveData = MutableLiveData<String>()
    val websiteLiveData: LiveData<String> = _websiteLiveData

    private val _versionLiveData = MutableLiveData<String>()
    val versionLiveData: LiveData<String> = _versionLiveData

    private val _telegramLiveData = MutableLiveData<String>()
    val telegramLiveData: LiveData<String> = _telegramLiveData

    private val _emailLiveData = MutableLiveData<String>()
    val emailLiveData: LiveData<String> = _emailLiveData

    private val _openSendEmailEvent = MutableLiveData<Event<String>>()
    val openSendEmailEvent: LiveData<Event<String>> = _openSendEmailEvent

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    init {
        _websiteLiveData.value = BuildConfig.WEBSITE_URL

        val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        _versionLiveData.value = "${resourceManager.getString(R.string.about_version)} $versionName"

        _telegramLiveData.value = BuildConfig.TELEGRAM_URL
        _emailLiveData.value = BuildConfig.EMAIL
    }

    fun backButtonPressed() {
        router.back()
    }

    fun websiteClicked() {
        openBrowserEvent.value = Event(BuildConfig.WEBSITE_URL)
    }

    fun githubClicked() {
        openBrowserEvent.value = Event(BuildConfig.GITHUB_URL)
    }

    fun telegramClicked() {
        openBrowserEvent.value = Event(BuildConfig.TELEGRAM_URL)
    }

    fun emailClicked() {
        _openSendEmailEvent.value = Event(BuildConfig.EMAIL)
    }

    fun termsClicked() {
        openBrowserEvent.value = Event(appLinksProvider.termsUrl)
    }

    fun privacyClicked() {
        openBrowserEvent.value = Event(appLinksProvider.privacyUrl)
    }
}
