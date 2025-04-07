package io.novafoundation.nova.feature_push_notifications.presentation.welcome

import android.Manifest
import android.os.Build
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.common.mixin.api.Retriable
import io.novafoundation.nova.common.mixin.api.RetryPayload
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.permissions.PermissionsAsker
import io.novafoundation.nova.common.view.ButtonState
import io.novafoundation.nova.feature_push_notifications.R
import io.novafoundation.nova.feature_push_notifications.PushNotificationsRouter
import io.novafoundation.nova.feature_push_notifications.domain.interactor.PushNotificationsInteractor
import io.novafoundation.nova.feature_push_notifications.domain.interactor.WelcomePushNotificationsInteractor
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class PushWelcomeViewModel(
    private val router: PushNotificationsRouter,
    private val pushNotificationsInteractor: PushNotificationsInteractor,
    private val welcomePushNotificationsInteractor: WelcomePushNotificationsInteractor,
    private val permissionsAsker: PermissionsAsker.Presentation,
    private val resourceManager: ResourceManager,
    private val appLinksProvider: AppLinksProvider
) : BaseViewModel(), PermissionsAsker by permissionsAsker, Retriable, Browserable {

    private val _enablingInProgress = MutableStateFlow(false)

    override val retryEvent: MutableLiveData<Event<RetryPayload>> = MutableLiveData()

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    val buttonState = _enablingInProgress.map { inProgress ->
        when (inProgress) {
            true -> ButtonState.PROGRESS
            false -> ButtonState.NORMAL
        }
    }

    fun backClicked() {
        welcomePushNotificationsInteractor.setWelcomeScreenShown()
        router.back()
    }

    fun termsClicked() {
        openBrowserEvent.value = Event(appLinksProvider.termsUrl)
    }

    fun privacyClicked() {
        openBrowserEvent.value = Event(appLinksProvider.privacyUrl)
    }

    fun askPermissionAndEnablePushNotifications() {
        launch {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val isPermissionsGranted = permissionsAsker.requirePermissions(Manifest.permission.POST_NOTIFICATIONS)

                if (!isPermissionsGranted) {
                    return@launch
                }
            }

            _enablingInProgress.value = true
            pushNotificationsInteractor.initPushSettings()
                .onSuccess {
                    welcomePushNotificationsInteractor.setWelcomeScreenShown()
                    router.back()
                }
                .onFailure {
                    when (it) {
                        is TimeoutCancellationException -> showError(
                            resourceManager.getString(R.string.common_something_went_wrong_title),
                            resourceManager.getString(R.string.push_welcome_timeout_error_message)
                        )

                        else -> retryDialog()
                    }
                }

            _enablingInProgress.value = false
        }
    }

    private fun retryDialog() {
        retryEvent.value = Event(
            RetryPayload(
                title = resourceManager.getString(R.string.common_error_general_title),
                message = resourceManager.getString(R.string.common_retry_message),
                onRetry = { askPermissionAndEnablePushNotifications() }
            )
        )
    }
}
